package mx.tecnologiccorp.sigit.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import mx.tecnologiccorp.sigit.PruebaIntegracion;

/**
 * Tarea 7. Cambio 1: el cliente pidió poder registrar usuarios e iniciar sesión con correos de Gmail. Se
 * aceptan como cualquier otro dominio, comparados sin mayúsculas ni espacios al inicio o al final, y el
 * correo sigue siendo único. Cambio 2: el logotipo y los colores del cliente en el inicio de sesión y en
 * el encabezado de todas las páginas.
 */
class AutenticacionTarea7Test extends PruebaIntegracion {

	private Usuario administrador;

	@BeforeEach
	void crearAdministrador() {
		administrador = crearUsuario(Rol.ADMINISTRADOR, "admin.t7@sigit.test");
	}

	/** Alta por P-11, como la hace el Administrador. */
	private ResultActions alta(String correo) throws Exception {
		return mvc.perform(post("/usuarios").with(csrf())
			.with(como(administrador))
			.param("nombre", "Persona de prueba")
			.param("correo", correo)
			.param("rolId", String.valueOf(rol(Rol.EJECUTIVO).getId()))
			.param("areaId", "")
			.param("password", CLAVE));
	}

	@ParameterizedTest
	@ValueSource(strings = { "juan.perez@gmail.com", "maria.lopez@googlemail.com" })
	void seRegistraUnUsuarioConCorreoDeGmail(String correo) throws Exception {
		alta(correo).andExpect(redirectedUrl("/usuarios"));

		assertThat(usuarios.findByCorreo(correo)).isPresent();
	}

	@Test
	void elCorreoSeGuardaSinEspaciosYEnMinusculas() throws Exception {
		alta("  Juan.Perez@GMAIL.com  ").andExpect(redirectedUrl("/usuarios"));

		assertThat(usuarios.findByCorreo("juan.perez@gmail.com")).isPresent();
	}

	@ParameterizedTest
	@ValueSource(strings = { "juan.perez@gmail.com", "Juan.Perez@Gmail.com", "  JUAN.PEREZ@GMAIL.COM  " })
	void seIniciaSesionConGmailComoSeEscriba(String escrito) throws Exception {
		alta("juan.perez@gmail.com").andExpect(redirectedUrl("/usuarios"));

		assertThat(entrar(escrito, CLAVE).getResponse().getRedirectedUrl()).isEqualTo("/tablero");
	}

	@Test
	void noSeRegistraDosVecesElMismoCorreoConOtraCapitalizacion() throws Exception {
		alta("juan.perez@gmail.com").andExpect(redirectedUrl("/usuarios"));

		alta(" JUAN.Perez@Gmail.COM ").andExpect(status().isOk())
			.andExpect(content().string(Matchers.containsString("Ya existe un usuario con este correo.")));
		assertThat(usuarios.count()).isEqualTo(2);
	}

	@Test
	void ningunaParteDelCodigoGuardaUnCorreoSinNormalizar() {
		// Defensa adicional: aunque un correo no llegue por el formulario, la entidad lo normaliza.
		Usuario usuario = new Usuario("Persona", "  Ana.Ruiz@Gmail.COM ", "x", rol(Rol.EJECUTIVO), null);
		assertThat(usuario.getCorreo()).isEqualTo("ana.ruiz@gmail.com");

		usuario.setCorreo(" Otra@GoogleMail.com");
		assertThat(usuario.getCorreo()).isEqualTo("otra@googlemail.com");
	}

	@Test
	void elBloqueoTrasCincoIntentosSigueFuncionandoConGmail() throws Exception {
		alta("juan.perez@gmail.com").andExpect(redirectedUrl("/usuarios"));

		for (int i = 0; i < 5; i++) {
			// Mezcla de formas de escribirlo: todas cuentan para la misma cuenta.
			entrar(i % 2 == 0 ? "juan.perez@gmail.com" : " Juan.Perez@GMAIL.com", "incorrecta");
		}

		assertThat(entrar("juan.perez@gmail.com", CLAVE).getResponse().getRedirectedUrl()).isEqualTo("/login");
		assertThat(loginTras(entrar("juan.perez@gmail.com", CLAVE))).contains("Demasiados intentos fallidos.");
	}

	// ---------- Cambio 2: logotipo y colores ----------

	@Test
	void elInicioDeSesionMuestraElLogotipoYCargaElTema() throws Exception {
		String login = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn().getResponse()
			.getContentAsString();

		assertThat(login).contains("src=\"/img/logo.png\"", "alt=\"H. Ayuntamiento de Benito Juárez 2026-2029\"",
				"href=\"/img/favicon.png\"", "href=\"/img/favicon-180.png\"")
			.doesNotContain("bi-laptop");
		assertThat(login.indexOf("/css/tema.css")).as("tema.css antes que sigit.css").isPositive()
			.isLessThan(login.indexOf("/css/sigit.css"));
	}

	@Test
	void elEncabezadoDeLasPaginasMuestraElLogotipo() throws Exception {
		for (String ruta : new String[] { "/equipos", "/usuarios" }) {
			String pagina = mvc.perform(get(ruta).with(como(administrador))).andExpect(status().isOk()).andReturn()
				.getResponse()
				.getContentAsString();
			// En el menú lateral (escritorio) y en el encabezado (celular).
			assertThat(pagina).as(ruta).contains("class=\"app-marca__logo\"", "app-encabezado__logo", "src=\"/img/logo.png\"",
					"SIGIT, ir al inicio", "/css/tema.css", "/img/favicon.png");
		}
	}

	@Test
	void elLogotipoElFaviconYElTemaSeDescarganSinSesion() throws Exception {
		mvc.perform(get("/img/logo.png")).andExpect(status().isOk())
			.andExpect(content().contentType("image/png"));
		mvc.perform(get("/img/favicon.png")).andExpect(status().isOk());
		mvc.perform(get("/img/favicon-180.png")).andExpect(status().isOk());
		mvc.perform(get("/css/tema.css")).andExpect(status().isOk())
			.andExpect(content().string(Matchers.containsString("--sigit-primario:")))
			.andExpect(content().string(Matchers.containsString("--bs-primary: var(--sigit-primario)")));
	}

}
