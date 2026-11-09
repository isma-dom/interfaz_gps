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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import mx.tecnologiccorp.sigit.PruebaIntegracion;
import mx.tecnologiccorp.sigit.inventario.EstadoEquipo;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.Usuario;

/** Prototipo de la Tarea 1 (P-03 y P-05): solo existe con el perfil dev. */
@ActiveProfiles("dev")
class PrototipoEquiposTest extends PruebaIntegracion {

	@Autowired
	private MuestraDeInventario inventario;

	private MuestraDeInventario.Equipo equipo(String numInventario) {
		return inventario.buscar(numInventario, null, null, null, 0).getContent().get(0);
	}

	private MockHttpServletRequestBuilder baja(String numInventario, String motivo) {
		return post("/prototipos/equipos/" + equipo(numInventario).id() + "/baja").with(csrf()).param("motivo", motivo);
	}

	private MockHttpServletRequestBuilder registro(String inventario, String serie) {
		return post("/prototipos/equipos").with(csrf())
			.param("numInventario", inventario)
			.param("numSerie", serie)
			.param("nombre", "PC de préstamo")
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
			.andExpect(content().string(containsString("Nuevo equipo")))
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
			.andExpect(content().string(containsString("Revisa 8 campos")))
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
			.andExpect(content().string(not(containsString("Nuevo equipo"))));
		mvc.perform(get("/prototipos/equipos/1/editar").with(como(ejecutivo))).andExpect(status().isForbidden());
	}


	/** RF-02 y RN-04: la baja exige el estado correcto, motivo y que no haya resguardo activo. */
	@Test
	void unCandidatoABajaSinResguardoSeDaDeBajaConMotivo() throws Exception {
		mvc.perform(baja("BBJ-0013", "Tarjeta madre dañada, no conviene repararla.")
			.with(como(crearUsuario(Rol.ADMINISTRADOR))))
			.andExpect(redirectedUrl("/prototipos/equipos"))
			.andExpect(flash().attribute("exito", "Equipo BBJ-0013 dado de baja."));

		assertThat(equipo("BBJ-0013").estado()).isEqualTo(EstadoEquipo.BAJA);
	}

	@Test
	void laBajaExigeMotivo() throws Exception {
		mvc.perform(baja("BBJ-0033", "").with(como(crearUsuario(Rol.ADMINISTRADOR))))
			.andExpect(flash().attribute("error", "Escribe el motivo de la baja, con al menos 10 caracteres."));

		assertThat(equipo("BBJ-0033").estado()).isEqualTo(EstadoEquipo.REVISION_PENDIENTE);
	}

	@Test
	void unEquipoConResguardoActivoNoSeDaDeBaja() throws Exception {
		mvc.perform(baja("BBJ-0010", "Equipo obsoleto con Windows 7.").with(como(crearUsuario(Rol.ADMINISTRADOR))))
			.andExpect(flash().attribute("error", "Primero libera el resguardo de Mario Canché Uc."));

		assertThat(equipo("BBJ-0010").estado()).isEqualTo(EstadoEquipo.CANDIDATO_BAJA);
	}

	@Test
	void unEquipoOperativoNoSeDaDeBajaDirectamente() throws Exception {
		mvc.perform(baja("BBJ-0004", "Se descompuso el escáner.").with(como(crearUsuario(Rol.ADMINISTRADOR))))
			.andExpect(flash().attribute("error",
					"Solo se puede dar de baja un equipo Candidato a baja o en Revisión pendiente."));

		assertThat(equipo("BBJ-0004").estado()).isEqualTo(EstadoEquipo.OPERATIVO);
	}

	@Test
	void soloElAdministradorDaDeBaja() throws Exception {
		mvc.perform(baja("BBJ-0033", "No se localizó en el levantamiento.").with(como(crearUsuario(Rol.EJECUTIVO))))
			.andExpect(status().isForbidden());
	}

}
