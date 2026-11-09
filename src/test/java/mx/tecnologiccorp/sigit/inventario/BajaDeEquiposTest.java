package mx.tecnologiccorp.sigit.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import mx.tecnologiccorp.sigit.PruebaIntegracion;
import mx.tecnologiccorp.sigit.catalogos.Area;
import mx.tecnologiccorp.sigit.catalogos.MarcaRepository;
import mx.tecnologiccorp.sigit.catalogos.TipoEquipoRepository;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.Usuario;

/** Tarea 4: baja de equipos (RF-02, RN-04, AD 2.2.3) y la clasificación que la habilita (RN-06). */
class BajaDeEquiposTest extends PruebaIntegracion {

	private static final String MOTIVO = "Tarjeta madre dañada; la reparación cuesta más que el equipo.";

	@Autowired
	private EquipoService servicio;

	@Autowired
	private ClasificadorDeObsolescencia clasificador;

	@Autowired
	private TipoEquipoRepository tipos;

	@Autowired
	private MarcaRepository marcas;

	@Autowired
	private JdbcTemplate jdbc;

	private Usuario administrador;

	private Area sala;

	@BeforeEach
	void datos() {
		administrador = crearUsuario(Rol.ADMINISTRADOR, "admin.bajas@sigit.test");
		sala = crearArea("Sala de consulta");
	}

	/** Una computadora registrada por el Administrador (estado Operativo, con su movimiento de alta). */
	private Equipo registrar(String inventario, String so, LocalDate adquirida) {
		EquipoForm f = new EquipoForm();
		f.setNumInventario(inventario);
		f.setNumSerie("S-" + inventario);
		f.setNombre("PC " + inventario);
		f.setTipoId(tipos.findAll().stream().filter(t -> t.getNombre().equals("Computadora de escritorio")).findFirst()
			.orElseThrow().getId());
		f.setMarcaId(marcas.findAll().stream().filter(m -> m.getNombre().equals("HP")).findFirst().orElseThrow()
			.getId());
		f.setModelo("ProDesk 400 G4");
		f.setAreaId(sala.getId());
		f.setProcesador("Core i5-7500");
		f.setMemoriaGb(8);
		f.setSistemaOperativo(so);
		f.setFechaAdquisicion(adquirida);
		return servicio.registrar(f, administrador.getId());
	}

	/** Un equipo Candidato a baja, como lo deja la clasificación de obsolescencia. */
	private Equipo candidato(String inventario) {
		Equipo equipo = registrar(inventario, "Windows 10", LocalDate.of(2024, 1, 15));
		clasificador.clasificar();
		return recargar(equipo);
	}

	private Equipo recargar(Equipo equipo) {
		return equipos.findById(equipo.getId()).orElseThrow();
	}

	private MockHttpServletRequestBuilder baja(Equipo equipo, Usuario quien, String motivo, boolean confirmo) {
		MockHttpServletRequestBuilder peticion = post("/equipos/" + equipo.getId() + "/baja").with(csrf())
			.with(como(quien))
			.param("version", String.valueOf(equipo.getVersion()));
		if (motivo != null) {
			peticion.param("motivo", motivo);
		}
		if (confirmo) {
			peticion.param("confirmo", "true");
		}
		return peticion;
	}

	private String estadoEnMysql(Equipo equipo) {
		return jdbc.queryForObject("select estado from equipos where id = ?", String.class, equipo.getId());
	}

	private List<Map<String, Object>> movimientosEnMysql(Equipo equipo, String tipo) {
		return jdbc.queryForList("select * from movimientos where equipo_id = ? and tipo = ?", equipo.getId(), tipo);
	}

	private String pagina(String url, Usuario quien) throws Exception {
		return mvc.perform(get(url).with(como(quien))).andExpect(status().isOk()).andReturn().getResponse()
			.getContentAsString();
	}

	// ---------- Cómo llega un equipo a Candidato a baja (RN-06) ----------

	@Test
	void laClasificacionMarcaComoCandidatosLosEquiposObsoletos() {
		Equipo sinSoporte = registrar("BBJ-0301", "Windows 10", LocalDate.of(2024, 1, 15));
		Equipo antiguo = registrar("BBJ-0302", "Windows 11", LocalDate.of(2021, 3, 1));
		Equipo vigente = registrar("BBJ-0303", "Windows 11", LocalDate.of(2025, 6, 1));
		Equipo enReparacion = registrar("BBJ-0304", "Windows 7", LocalDate.of(2018, 1, 1));
		jdbc.update("update equipos set estado = 'en_reparacion' where id = ?", enReparacion.getId());

		assertThat(clasificador.clasificar()).isEqualTo(2);

		assertThat(recargar(sinSoporte).getEstado()).isEqualTo(EstadoEquipo.CANDIDATO_BAJA);
		assertThat(recargar(antiguo).getEstado()).isEqualTo(EstadoEquipo.CANDIDATO_BAJA);
		assertThat(recargar(vigente).getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);
		// La regla solo aplica a Operativo e Inactivo (AD 2.2.3).
		assertThat(recargar(enReparacion).getEstado()).isEqualTo(EstadoEquipo.EN_REPARACION);
		assertThat(movimientosEnMysql(sinSoporte, "cambio_estado")).singleElement().satisfies(m -> {
			assertThat(m.get("estado_anterior")).isEqualTo("operativo");
			assertThat(m.get("estado_nuevo")).isEqualTo("candidato_baja");
			assertThat(m.get("usuario_id")).isNull();
			assertThat((String) m.get("motivo")).contains("Windows 10", "ya no tiene soporte");
		});
		assertThat((String) movimientosEnMysql(antiguo, "cambio_estado").get(0).get("motivo"))
			.contains("5 años de antigüedad");
		// Correrla otra vez no repite movimientos.
		assertThat(clasificador.clasificar()).isZero();
	}

	@Test
	void elAltaDejaSuMovimientoEnElHistorial() {
		Equipo equipo = registrar("BBJ-0301", "Windows 11", LocalDate.of(2025, 6, 1));

		assertThat(movimientosEnMysql(equipo, "alta")).singleElement().satisfies(m -> {
			assertThat(m.get("estado_nuevo")).isEqualTo("operativo");
			assertThat(((Number) m.get("usuario_id")).longValue()).isEqualTo(administrador.getId());
			assertThat(((Number) m.get("area_destino_id")).longValue()).isEqualTo(sala.getId());
		});
	}

	// ---------- Quién puede y confirmación ----------

	@Test
	void soloElAdministradorDaDeBaja() throws Exception {
		Equipo equipo = candidato("BBJ-0301");
		for (String rol : new String[] { Rol.EJECUTIVO, Rol.CONSULTA }) {
			Usuario usuario = crearUsuario(rol);
			mvc.perform(get("/equipos/" + equipo.getId() + "/baja").with(como(usuario)))
				.andExpect(status().isForbidden());
			mvc.perform(baja(equipo, usuario, MOTIVO, true)).andExpect(status().isForbidden());
		}
		assertThat(pagina("/equipos/" + equipo.getId(), crearUsuario(Rol.EJECUTIVO))).doesNotContain("/baja");

		assertThat(estadoEnMysql(equipo)).isEqualTo("candidato_baja");
		assertThat(movimientosEnMysql(equipo, "baja")).isEmpty();
	}

	@Test
	void antesDeLaBajaElSistemaPideConfirmacionYNoCambiaNada() throws Exception {
		Equipo equipo = candidato("BBJ-0301");

		assertThat(pagina("/equipos/" + equipo.getId(), administrador)).contains("/equipos/" + equipo.getId() + "/baja",
				"Dar de baja");
		assertThat(pagina("/equipos/" + equipo.getId() + "/baja", administrador)).contains("Dar de baja BBJ-0301",
				"¿Seguro que quieres dar de baja este equipo?", "La baja es definitiva", "PC BBJ-0301", "S-BBJ-0301",
				"Candidato a baja", "Motivo de la baja", "se da de baja de forma definitiva", "Confirmar baja",
				"Cancelar");

		assertThat(estadoEnMysql(equipo)).isEqualTo("candidato_baja");
	}

	@Test
	void sinMotivoOSinConfirmarNoSeRealiza() throws Exception {
		Equipo equipo = candidato("BBJ-0301");

		mvc.perform(baja(equipo, administrador, null, false))
			.andExpect(status().isOk())
			.andExpect(content().string(allOf(containsString("Revisa 2 campos"),
					containsString("Escribe el motivo de la baja."),
					containsString("Marca la casilla para confirmar que la baja es definitiva."))));
		mvc.perform(baja(equipo, administrador, "Vieja", true))
			.andExpect(content().string(containsString("El motivo debe tener entre 10 y 500 caracteres.")));
		mvc.perform(baja(equipo, administrador, MOTIVO, false))
			// Lo escrito se conserva para no volver a teclearlo.
			.andExpect(content().string(allOf(containsString("Revisa 1 campo"), containsString("Tarjeta madre"))));

		assertThat(estadoEnMysql(equipo)).isEqualTo("candidato_baja");
		assertThat(movimientosEnMysql(equipo, "baja")).isEmpty();
	}

	// ---------- La baja ----------

	@Test
	void laBajaCambiaElEstadoYQuedaRegistradaEnMysql() throws Exception {
		Equipo equipo = candidato("BBJ-0301");

		MvcResult resultado = mvc.perform(baja(equipo, administrador, "  " + MOTIVO + " ", true))
			.andExpect(redirectedUrl("/equipos/" + equipo.getId()))
			.andReturn();

		EquipoDadoDeBaja confirmacion = (EquipoDadoDeBaja) resultado.getFlashMap().get("dadoDeBaja");
		assertThat(confirmacion.numInventario()).isEqualTo("BBJ-0301");
		assertThat(confirmacion.estadoAnterior()).isEqualTo(EstadoEquipo.CANDIDATO_BAJA);
		assertThat(estadoEnMysql(equipo)).isEqualTo("baja");
		assertThat(movimientosEnMysql(equipo, "baja")).singleElement().satisfies(m -> {
			assertThat(m.get("estado_anterior")).isEqualTo("candidato_baja");
			assertThat(m.get("estado_nuevo")).isEqualTo("baja");
			assertThat(m.get("motivo")).isEqualTo(MOTIVO);
			assertThat(((Number) m.get("usuario_id")).longValue()).isEqualTo(administrador.getId());
			assertThat(m.get("fecha")).isEqualTo(LocalDateTime.now(reloj));
		});
		Equipo guardado = recargar(equipo);
		assertThat(guardado.getVersion()).isEqualTo(equipo.getVersion() + 1);
	}

	@Test
	void elSistemaConfirmaQueLaBajaSeRealizo() throws Exception {
		Equipo equipo = candidato("BBJ-0301");
		MvcResult resultado = mvc.perform(baja(equipo, administrador, MOTIVO, true)).andReturn();
		MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession();

		mvc.perform(get("/equipos/" + equipo.getId()).session(sesion).with(como(administrador)))
			.andExpect(content().string(allOf(containsString("Se dio de baja el equipo BBJ-0301"),
					containsString("Candidato a baja"), containsString(MOTIVO),
					containsString("Ya no aparece entre los equipos activos"))));
	}

	@Test
	void unEquipoDadoDeBajaDejaDeAparecerComoActivo() throws Exception {
		Equipo dadoDeBaja = candidato("BBJ-0301");
		registrar("BBJ-0302", "Windows 11", LocalDate.of(2025, 6, 1));
		mvc.perform(baja(dadoDeBaja, administrador, MOTIVO, true));

		assertThat(pagina("/equipos", administrador)).contains("Se encontró 1 equipo", ">BBJ-0302<",
				"No se muestra 1 equipo dado de baja.").doesNotContain(">BBJ-0301<");
		assertThat(pagina("/equipos?q=BBJ-0301", administrador)).contains("Ningún equipo coincide");
		assertThat(pagina("/equipos?estado=BAJA", administrador)).contains("Se encontró 1 equipo", ">BBJ-0301<")
			.doesNotContain(">BBJ-0302<");
		assertThat(pagina("/equipos?estado=TODOS", administrador)).contains("Mostrando 1 a 2 de 2 equipos");
	}

	@Test
	void laInformacionDelEquipoSeConservaParaFuturasConsultas() throws Exception {
		Equipo equipo = candidato("BBJ-0301");
		mvc.perform(baja(equipo, administrador, MOTIVO, true));

		Equipo guardado = recargar(equipo);
		assertThat(guardado.getNombre()).isEqualTo("PC BBJ-0301");
		assertThat(guardado.getNumSerie()).isEqualTo("S-BBJ-0301");
		assertThat(guardado.getQrUuid()).isEqualTo(equipo.getQrUuid());
		assertThat(guardado.getCaracteristicas()).containsEntry("procesador", "Core i5-7500");
		assertThat(guardado.getFechaAdquisicion()).isEqualTo(LocalDate.of(2024, 1, 15));

		String ficha = pagina("/equipos/" + equipo.getId(), crearUsuario(Rol.EJECUTIVO));
		assertThat(ficha).contains("Equipo dado de baja", "Persona admin.bajas@sigit.test", MOTIVO, "PC BBJ-0301",
				"Core i5-7500", "Historial de movimientos", "Alta", "Cambio de estado", "ya no tiene soporte")
			.doesNotContain("/editar");
		assertThat(ficha.indexOf("Tarjeta madre")).isLessThan(ficha.indexOf("Registró: Persona admin"));
		// Ni editar ni otra baja.
		mvc.perform(get("/equipos/" + equipo.getId() + "/editar").with(como(administrador)))
			.andExpect(redirectedUrl("/equipos/" + equipo.getId()));
		mvc.perform(baja(recargar(equipo), administrador, MOTIVO, true))
			.andExpect(flash().attribute("error", "Este equipo ya está dado de baja."));
		assertThat(movimientosEnMysql(equipo, "baja")).hasSize(1);
	}

	// ---------- Reglas de la máquina de estados ----------

	@Test
	void unEquipoOperativoNoSePuedeDarDeBaja() throws Exception {
		Equipo equipo = registrar("BBJ-0301", "Windows 11", LocalDate.of(2025, 6, 1));

		mvc.perform(get("/equipos/" + equipo.getId() + "/baja").with(como(administrador)))
			.andExpect(redirectedUrl("/equipos/" + equipo.getId()))
			.andExpect(flash().attribute("error", containsString("Este equipo está Operativo.")));
		mvc.perform(baja(equipo, administrador, MOTIVO, true))
			.andExpect(flash().attribute("error", containsString("Solo se puede dar de baja un equipo Candidato")));
		assertThat(pagina("/equipos/" + equipo.getId(), administrador)).doesNotContain("/baja\"");

		assertThat(estadoEnMysql(equipo)).isEqualTo("operativo");
	}

	@Test
	void unEquipoEnRevisionPendienteTambienSePuedeDarDeBaja() throws Exception {
		Equipo equipo = registrar("BBJ-0301", "Windows 11", LocalDate.of(2025, 6, 1));
		jdbc.update("update equipos set estado = 'revision_pendiente' where id = ?", equipo.getId());

		mvc.perform(baja(equipo, administrador, "No se localizó en el levantamiento físico.", true))
			.andExpect(redirectedUrl("/equipos/" + equipo.getId()))
			.andExpect(flash().attributeExists("dadoDeBaja"));

		assertThat(movimientosEnMysql(equipo, "baja").get(0).get("estado_anterior")).isEqualTo("revision_pendiente");
	}

	@Test
	void siElEquipoCambioMientrasSeConfirmabaNoSeDaDeBaja() throws Exception {
		Equipo equipo = candidato("BBJ-0301");
		// Alguien lo guardó después de abrir la confirmación: la versión ya no coincide.
		jdbc.update("update equipos set version = version + 1 where id = ?", equipo.getId());

		mvc.perform(baja(equipo, administrador, MOTIVO, true))
			.andExpect(flash().attribute("error", containsString("Alguien más modificó este equipo")));

		assertThat(estadoEnMysql(equipo)).isEqualTo("candidato_baja");
	}

	@Test
	void laBaseDeDatosExigeElMotivoDeLaBaja() {
		Equipo equipo = candidato("BBJ-0301");

		assertThatThrownBy(() -> jdbc.update("""
				insert into movimientos (equipo_id, tipo, estado_anterior, estado_nuevo, fecha)
				values (?, 'baja', 'candidato_baja', 'baja', now())
				""", equipo.getId())).isInstanceOf(DataAccessException.class)
			.hasMessageContaining("ck_movimientos_motivo_baja");
	}

}
