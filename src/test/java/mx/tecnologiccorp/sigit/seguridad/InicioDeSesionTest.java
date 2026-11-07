package mx.tecnologiccorp.sigit.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import mx.tecnologiccorp.sigit.PruebaIntegracion;

class InicioDeSesionTest extends PruebaIntegracion {

	private static final List<String> MENU = List.of("Tablero", "Equipos", "Escaneo de QR", "Resguardos",
			"Importación y cuarentena", "Reportes", "Catálogos", "Usuarios", "Bitácora");

	static Stream<Arguments> menuPorRol() {
		return Stream.of(Arguments.of(Rol.ADMINISTRADOR, "/tablero", MENU),
				Arguments.of(Rol.EJECUTIVO, "/tablero",
						List.of("Tablero", "Equipos", "Escaneo de QR", "Resguardos", "Reportes")),
				Arguments.of(Rol.CONSULTA, "/equipos", List.of("Equipos", "Escaneo de QR", "Resguardos", "Reportes")));
	}

	/** CP-01: cada rol entra y ve solo el menú que le corresponde. */
	@ParameterizedTest(name = "{0}")
	@MethodSource("menuPorRol")
	void cadaRolEntraYVeSoloSuMenu(String rol, String destino, List<String> menu) throws Exception {
		Usuario usuario = crearUsuario(rol);

		MvcResult resultado = entrar(usuario.getCorreo(), CLAVE);
		assertThat(resultado.getResponse().getRedirectedUrl()).isEqualTo(destino);

		String html = mvc.perform(get(destino).session(sesionDe(resultado)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String nav = html.substring(html.indexOf("<nav aria-label=\"Menú principal\">"), html.indexOf("</nav>"));
		for (String entrada : MENU) {
			if (menu.contains(entrada)) {
				assertThat(nav).contains(">" + entrada + "<");
			}
			else {
				assertThat(nav).doesNotContain(">" + entrada + "<");
			}
		}
	}

	@Test
	void correoConMayusculasYEspaciosIniciaSesion() throws Exception {
		crearUsuario(Rol.EJECUTIVO, "ana@sigit.test");

		MvcResult resultado = entrar("  ANA@sigit.test ", CLAVE);

		assertThat(resultado.getResponse().getRedirectedUrl()).isEqualTo("/tablero");
	}

	@Test
	void credencialesIncorrectasMuestranMensajeGenerico() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);

		MvcResult resultado = entrar(usuario.getCorreo(), "otra");

		assertThat(resultado.getResponse().getRedirectedUrl()).isEqualTo("/login");
		assertThat(loginTras(resultado)).contains("Correo o contraseña incorrectos.")
			.contains("value=\"" + usuario.getCorreo() + "\"");
	}

	@Test
	void correoInexistenteEInactivoDanElMismoMensaje() throws Exception {
		Usuario inactivo = crearUsuario(Rol.EJECUTIVO);
		inactivo.setActivo(false);
		usuarios.save(inactivo);

		assertThat(loginTras(entrar("nadie@sigit.test", CLAVE))).contains("Correo o contraseña incorrectos.");
		assertThat(loginTras(entrar(inactivo.getCorreo(), CLAVE))).contains("Correo o contraseña incorrectos.");
	}

	@Test
	void camposVaciosMuestranErroresPorCampo() throws Exception {
		assertThat(loginTras(entrar("", ""))).contains("Escribe el correo.").contains("Escribe la contraseña.");
	}

	@Test
	void rutaProtegidaSinSesionVuelveAEsaRutaAlEntrar() throws Exception {
		Usuario administrador = crearUsuario(Rol.ADMINISTRADOR);
		MvcResult sinSesion = mvc.perform(get("/usuarios")).andExpect(redirectedUrl("/login")).andReturn();
		MockHttpSession sesion = (MockHttpSession) sinSesion.getRequest().getSession();

		mvc.perform(post("/login").session(sesion)
			.with(csrf())
			.param("correo", administrador.getCorreo())
			.param("password", CLAVE)).andExpect(redirectedUrl("http://localhost/usuarios?continue"));
	}

	@Test
	void usuarioConSesionQueAbreLoginVaASuInicio() throws Exception {
		mvc.perform(get("/login").with(como(crearUsuario(Rol.CONSULTA)))).andExpect(redirectedUrl("/equipos"));
		mvc.perform(get("/login").with(como(crearUsuario(Rol.EJECUTIVO)))).andExpect(redirectedUrl("/tablero"));
	}

	@Test
	void laRaizLlevaAlInicioDelRol() throws Exception {
		mvc.perform(get("/").with(como(crearUsuario(Rol.CONSULTA)))).andExpect(redirectedUrl("/equipos"));
	}

	@Test
	void cerrarSesionTerminaLaSesionYExigePost() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);
		MockHttpSession sesion = sesionDe(entrar(usuario.getCorreo(), CLAVE));

		mvc.perform(get("/logout").session(sesion));
		mvc.perform(get("/tablero").session(sesion)).andExpect(status().isOk()).andExpect(authenticated());

		mvc.perform(post("/logout").session(sesion).with(csrf())).andExpect(redirectedUrl("/login"));
		mvc.perform(get("/tablero").session(sesion))
			.andExpect(redirectedUrl("/login"))
			.andExpect(unauthenticated());
	}

	@Test
	void noExisteRecuerdameNiRecuperacionDeContrasena() throws Exception {
		mvc.perform(get("/login"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Inicia sesión")))
			.andExpect(content().string(not(containsString("Recuérdame"))))
			.andExpect(content().string(not(containsString("Olvidaste"))));
	}

	@Test
	void laContrasenaSeGuardaCifrada() {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);

		assertThat(usuario.getPassword()).startsWith("{bcrypt}").doesNotContain(CLAVE);
	}

}
