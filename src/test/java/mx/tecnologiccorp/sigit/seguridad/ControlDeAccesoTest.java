package mx.tecnologiccorp.sigit.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import mx.tecnologiccorp.sigit.PruebaIntegracion;

class ControlDeAccesoTest extends PruebaIntegracion {

	/** Matriz de AD 2.2.7. Se escribe aparte de Pantalla para detectar diferencias. */
	private static final Map<String, List<String>> MATRIZ = new LinkedHashMap<>();

	static {
		List<String> todos = List.of(Rol.ADMINISTRADOR, Rol.EJECUTIVO, Rol.CONSULTA);
		MATRIZ.put("/tablero", List.of(Rol.ADMINISTRADOR, Rol.EJECUTIVO));
		MATRIZ.put("/equipos", todos);
		MATRIZ.put("/escaneo", todos);
		MATRIZ.put("/resguardos", todos);
		MATRIZ.put("/importaciones", List.of(Rol.ADMINISTRADOR));
		MATRIZ.put("/reportes", todos);
		MATRIZ.put("/catalogos", List.of(Rol.ADMINISTRADOR));
		MATRIZ.put("/usuarios", List.of(Rol.ADMINISTRADOR));
		MATRIZ.put("/bitacora", List.of(Rol.ADMINISTRADOR));
	}

	static List<Arguments> rolesYRutas() {
		List<Arguments> casos = new ArrayList<>();
		MATRIZ.forEach((ruta, permitidos) -> {
			for (String rol : List.of(Rol.ADMINISTRADOR, Rol.EJECUTIVO, Rol.CONSULTA)) {
				casos.add(Arguments.of(rol, ruta, permitidos.contains(rol)));
			}
		});
		return casos;
	}

	@ParameterizedTest(name = "{0} en {1}: {2}")
	@MethodSource("rolesYRutas")
	void cadaRolAccedeSoloALoPermitido(String rol, String ruta, boolean permitido) throws Exception {
		mvc.perform(get(ruta).with(como(crearUsuario(rol))))
			.andExpect(permitido ? status().isOk() : status().isForbidden());
	}

	@ParameterizedTest
	@ValueSource(strings = { "/tablero", "/equipos", "/escaneo", "/resguardos", "/importaciones", "/reportes",
			"/catalogos", "/usuarios", "/usuarios/nuevo", "/bitacora", "/" })
	void sinSesionRedirigeAlLogin(String ruta) throws Exception {
		mvc.perform(get(ruta)).andExpect(redirectedUrl("/login"));
	}

	@Test
	void laMatrizCoincideConLasPantallas() {
		for (Pantalla pantalla : Pantalla.values()) {
			assertThat(pantalla.getRoles()).as(pantalla.getRuta())
				.containsExactlyInAnyOrderElementsOf(MATRIZ.get(pantalla.getRuta()));
		}
	}

	@Test
	void respondeJson403SiLaPeticionEsperaJson() throws Exception {
		mvc.perform(get("/usuarios").accept(MediaType.APPLICATION_JSON).with(como(crearUsuario(Rol.CONSULTA))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value("No tienes permiso para realizar esta acción."));
	}

	@Test
	void sinSesionUnaPeticionJsonRecibe401() throws Exception {
		mvc.perform(get("/usuarios").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}

	@Test
	void lasPaginasConSesionNoSeGuardanEnCache() throws Exception {
		String control = mvc.perform(get("/equipos").with(como(crearUsuario(Rol.EJECUTIVO))))
			.andReturn()
			.getResponse()
			.getHeader("Cache-Control");

		assertThat(control).contains("no-store");
	}

}
