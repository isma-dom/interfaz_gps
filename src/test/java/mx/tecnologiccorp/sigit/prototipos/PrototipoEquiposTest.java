package mx.tecnologiccorp.sigit.prototipos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import mx.tecnologiccorp.sigit.PruebaIntegracion;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.Usuario;

/** Prototipo de la Tarea 1 (P-03 y P-05): solo existe con el perfil dev. */
@ActiveProfiles("dev")
class PrototipoEquiposTest extends PruebaIntegracion {

	private MockHttpServletRequestBuilder registro(String inventario, String serie) {
		return post("/prototipos/equipos").with(csrf())
			.param("numInventario", inventario)
			.param("numSerie", serie)
			.param("tipoId", "1")
			.param("marcaId", "8")
			.param("modelo", "ProDesk 405 G8")
			.param("areaId", "5")
			.param("memoriaGb", "16");
	}

	@Test
	void elAdministradorVeElListadoConAccionesDeEdicion() throws Exception {
		mvc.perform(get("/prototipos/equipos").with(como(crearUsuario(Rol.ADMINISTRADOR))))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Registrar equipo")))
			.andExpect(content().string(containsString("data-seleccion-equipo")))
			.andExpect(content().string(containsString("Mostrando 1 a 15 de")));
	}

	@Test
	void registrarUnEquipoValidoVuelveAlListado() throws Exception {
		mvc.perform(registro("BBJ-9001", "SERIE-9001").with(como(crearUsuario(Rol.ADMINISTRADOR))))
			.andExpect(redirectedUrl("/prototipos/equipos"))
			.andExpect(flash().attribute("exito", "Equipo BBJ-9001 registrado."));
	}

	@Test
	void registrarYAgregarOtroConservaLoQueSeRepite() throws Exception {
		mvc.perform(registro("BBJ-9002", "SERIE-9002").param("otro", "1").with(como(crearUsuario(Rol.ADMINISTRADOR))))
			.andExpect(redirectedUrl("/prototipos/equipos/nuevo"))
			.andExpect(flash().attributeExists("equipo"));
	}

	@Test
	void losCamposObligatoriosYDuplicadosSeExplicanEnEspanol() throws Exception {
		mvc.perform(post("/prototipos/equipos").with(csrf())
			.param("numInventario", "bbj-0001")
			.param("costo", "-5")
			.with(como(crearUsuario(Rol.ADMINISTRADOR))))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Revisa 7 campos")))
			.andExpect(content().string(containsString("Ya existe un equipo con este número de inventario.")))
			.andExpect(content().string(containsString("El costo no puede ser negativo.")));
	}

	@Test
	void consultaSoloVeSuAreaYNoPuedeRegistrar() throws Exception {
		Usuario consulta = crearUsuario(Rol.CONSULTA, "consulta.prototipo@sigit.test");
		consulta.setArea(areas.save(new mx.tecnologiccorp.sigit.catalogos.Area("Sala de consulta", "Edificio principal", "1")));
		usuarios.save(consulta);

		mvc.perform(get("/prototipos/equipos").with(como(consulta)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Ves los equipos de")))
			.andExpect(content().string(containsString("BBJ-0030")))
			.andExpect(content().string(not(containsString("BBJ-0001"))))
			.andExpect(content().string(not(containsString("data-seleccion-equipo"))));
		mvc.perform(get("/prototipos/equipos/nuevo").with(como(consulta))).andExpect(status().isForbidden());
		mvc.perform(registro("BBJ-9003", "SERIE-9003").with(como(consulta))).andExpect(status().isForbidden());
	}

	@Test
	void ejecutivoConsultaPeroNoEdita() throws Exception {
		Usuario ejecutivo = crearUsuario(Rol.EJECUTIVO);

		mvc.perform(get("/prototipos/equipos").with(como(ejecutivo)))
			.andExpect(status().isOk())
			.andExpect(content().string(not(containsString("Registrar equipo"))));
		mvc.perform(get("/prototipos/equipos/1/editar").with(como(ejecutivo))).andExpect(status().isForbidden());
	}

}
