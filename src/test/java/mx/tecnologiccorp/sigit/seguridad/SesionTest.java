package mx.tecnologiccorp.sigit.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.mock.web.MockServletContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import mx.tecnologiccorp.sigit.PruebaIntegracion;

class SesionTest extends PruebaIntegracion {

	@Autowired
	private ServletContextInitializer configuracionDeSesion;

	/** Lo mismo que hace Tomcat externo al desplegar sigit.war. */
	@Test
	void laSesionDura30MinutosYLaCookieEsSegura() throws Exception {
		MockServletContext contexto = new MockServletContext();

		configuracionDeSesion.onStartup(contexto);

		assertThat(contexto.getSessionTimeout()).isEqualTo(30);
		assertThat(contexto.getSessionCookieConfig().isHttpOnly()).isTrue();
		assertThat(contexto.getSessionCookieConfig().getAttribute("SameSite")).isEqualTo("Lax");
		assertThat(contexto.getSessionCookieConfig().isSecure()).isFalse();
	}

	@Test
	void elLayoutRecibeLosDatosDelAvisoDeExpiracion() throws Exception {
		mvc.perform(get("/equipos").with(como(crearUsuario(Rol.EJECUTIVO))))
			.andExpect(content().string(containsString("data-sesion-minutos=\"30\"")))
			.andExpect(content().string(containsString("data-sesion-aviso-minutos=\"2\"")))
			.andExpect(content().string(containsString("Tu sesión está por terminar")));
	}

	@Test
	void seguirConectadoRespondeSinContenido() throws Exception {
		mvc.perform(post("/sesion/mantener").with(csrf()).with(como(crearUsuario(Rol.EJECUTIVO))))
			.andExpect(status().isNoContent());
	}

	@Test
	void seguirConectadoSinSesionRecibe401() throws Exception {
		mvc.perform(post("/sesion/mantener").with(csrf()).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void elLoginMuestraQueLaSesionExpiro() throws Exception {
		mvc.perform(get("/login?expirada=1")).andExpect(content().string(containsString("Tu sesión expiró por inactividad.")));
		mvc.perform(get("/login")).andExpect(content().string(not(containsString("Tu sesión expiró por inactividad."))));
	}

	@Test
	void unTokenCsrfVencidoLlevaAlLoginConElMensaje() throws Exception {
		MvcResult resultado = mvc.perform(post("/login").param("correo", "a@b.mx").param("password", "x"))
			.andExpect(redirectedUrl("/login"))
			.andReturn();

		assertThat(loginTras(resultado)).contains("Tu sesión expiró por inactividad.");
	}

	@Test
	void unTokenCsrfVencidoEnJsonRecibe403() throws Exception {
		mvc.perform(post("/sesion/mantener").accept(MediaType.APPLICATION_JSON).with(como(crearUsuario(Rol.EJECUTIVO))))
			.andExpect(status().isForbidden());
	}

	@Test
	void usuarioDesactivadoConSesionAbiertaPierdeElAcceso() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);
		MockHttpSession sesion = sesionDe(entrar(usuario.getCorreo(), CLAVE));
		mvc.perform(get("/equipos").session(sesion)).andExpect(status().isOk());

		usuario.setActivo(false);
		usuarios.save(usuario);

		MvcResult resultado = mvc.perform(get("/equipos").session(sesion))
			.andExpect(redirectedUrl("/login"))
			.andExpect(unauthenticated())
			.andReturn();
		assertThat(loginTras(resultado)).contains("Tu acceso fue desactivado. Pide ayuda a Soporte Técnico.");
	}

	@Test
	void usuarioDesactivadoEnJsonRecibe401() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);
		usuario.setActivo(false);
		usuarios.save(usuario);

		mvc.perform(get("/equipos").accept(MediaType.APPLICATION_JSON).with(como(usuario)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void unCambioDeRolSeAplicaSinVolverAEntrar() throws Exception {
		Usuario usuario = crearUsuario(Rol.ADMINISTRADOR);
		MockHttpSession sesion = sesionDe(entrar(usuario.getCorreo(), CLAVE));
		mvc.perform(get("/usuarios").session(sesion)).andExpect(status().isOk());

		usuario.setRol(rol(Rol.EJECUTIVO));
		usuarios.save(usuario);

		mvc.perform(get("/usuarios").session(sesion)).andExpect(status().isForbidden());
		mvc.perform(get("/tablero").session(sesion)).andExpect(status().isOk());
	}

}
