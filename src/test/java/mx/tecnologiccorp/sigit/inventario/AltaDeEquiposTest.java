package mx.tecnologiccorp.sigit.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
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

/** Tarea 2: alta de equipos (RF-02) guardando en MySQL 8.4. */
class AltaDeEquiposTest extends PruebaIntegracion {

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
		administrador = crearUsuario(Rol.ADMINISTRADOR, "admin.equipos@sigit.test");
		sala = crearArea("Sala de consulta");
	}

	private Long tipo(String nombre) {
		return tipos.findAll().stream().filter(t -> t.getNombre().equals(nombre)).findFirst().orElseThrow().getId();
	}

	private Long marca(String nombre) {
		return marcas.findAll().stream().filter(m -> m.getNombre().equals(nombre)).findFirst().orElseThrow().getId();
	}

	/**
	 * Una computadora con todos sus datos; cada par de {@code cambios} (campo, valor) reemplaza un dato.
	 * MockMvc agrega valores con param(), así que los cambios se aplican antes de enviarlos.
	 */
	private MockHttpServletRequestBuilder computadora(String inventario, String serie, String... cambios) {
		Map<String, String> datos = new LinkedHashMap<>();
		datos.put("numInventario", inventario);
		datos.put("numSerie", serie);
		datos.put("nombre", "PC de préstamo 3");
		datos.put("tipoId", tipo("Computadora de escritorio").toString());
		datos.put("marcaId", marca("HP").toString());
		datos.put("modelo", "ProDesk 405 G8");
		datos.put("areaId", sala.getId().toString());
		datos.put("procesador", "Ryzen 5 5600G");
		datos.put("memoriaGb", "16");
		datos.put("almacenamientoGb", "512");
		datos.put("tipoAlmacenamiento", "SSD");
		datos.put("sistemaOperativo", "Windows 11");
		datos.put("fechaAdquisicion", "2026-10-20");
		datos.put("costo", "15899.50");
		datos.put("observaciones", "Llegó con teclado y mouse.");
		for (int i = 0; i < cambios.length; i += 2) {
			datos.put(cambios[i], cambios[i + 1]);
		}
		MockHttpServletRequestBuilder peticion = post("/equipos").with(csrf()).with(como(administrador));
		datos.forEach(peticion::param);
		return peticion;
	}

	@Test
	void losCatalogosInicialesExisten() {
		assertThat(tipos.count()).isEqualTo(9);
		assertThat(marcas.count()).isEqualTo(11);
	}

	@Test
	void elAdministradorRegistraUnEquipoYSeGuardaEnLaBaseDeDatos() throws Exception {
		mvc.perform(computadora("bbj-0101", "mxl5120bq3"))
			.andExpect(redirectedUrl("/equipos"))
			.andExpect(flash().attributeExists("registrado"));

		Equipo equipo = equipos.findByNumInventario("BBJ-0101").orElseThrow();
		assertThat(equipo.getNumSerie()).isEqualTo("MXL5120BQ3");
		assertThat(equipo.getNombre()).isEqualTo("PC de préstamo 3");
		assertThat(equipo.getTipo().getNombre()).isEqualTo("Computadora de escritorio");
		assertThat(equipo.getMarca().getNombre()).isEqualTo("HP");
		assertThat(equipo.getModelo()).isEqualTo("ProDesk 405 G8");
		assertThat(equipo.getArea().getId()).isEqualTo(sala.getId());
		assertThat(equipo.getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);
		assertThat(equipo.getQrUuid()).hasSize(36);
		assertThat(equipo.getSistemaOperativo()).isEqualTo("Windows 11");
		assertThat(equipo.getFechaAdquisicion()).isEqualTo(LocalDate.of(2026, 10, 20));
		assertThat(equipo.getCosto()).isEqualByComparingTo(new BigDecimal("15899.50"));
		assertThat(equipo.getObservaciones()).isEqualTo("Llegó con teclado y mouse.");
		assertThat(equipo.getCaracteristicas()).containsEntry("procesador", "Ryzen 5 5600G")
			.containsEntry("memoriaGb", 16)
			.containsEntry("almacenamientoGb", 512)
			.containsEntry("tipoAlmacenamiento", "SSD");
	}

	@Test
	void enMysqlLasCaracteristicasQuedanComoJsonYElEstadoComoEnElDiccionario() throws Exception {
		mvc.perform(computadora("BBJ-0102", "SERIE-0102"));

		Map<String, Object> fila = jdbc.queryForMap(
				"select estado, json_extract(caracteristicas, '$.memoriaGb') as memoria from equipos where num_inventario = 'BBJ-0102'");
		assertThat(fila.get("estado")).isEqualTo("operativo");
		assertThat(fila.get("memoria").toString()).isEqualTo("16");
	}

	@Test
	void laConfirmacionMuestraLoQueSeGuardo() throws Exception {
		MvcResult resultado = mvc.perform(computadora("BBJ-0103", "SERIE-0103")).andReturn();
		MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession();

		mvc.perform(get("/equipos").session(sesion).with(como(administrador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Se registró el equipo BBJ-0103")))
			.andExpect(content().string(containsString("PC de préstamo 3")))
			.andExpect(content().string(containsString("Sala de consulta")))
			.andExpect(content().string(containsString("Hay 1 equipo registrado en el inventario.")));
	}

	@Test
	void registrarYAgregarOtroVuelveAlFormularioConLosDatosQueSeRepiten() throws Exception {
		MvcResult resultado = mvc.perform(computadora("BBJ-0104", "SERIE-0104").param("otro", "1"))
			.andExpect(redirectedUrl("/equipos/nuevo"))
			.andReturn();
		MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession();

		mvc.perform(get("/equipos/nuevo").session(sesion).with(como(administrador)))
			.andExpect(content().string(containsString("Se registró el equipo BBJ-0104")))
			.andExpect(content().string(containsString("value=\"ProDesk 405 G8\"")))
			.andExpect(content().string(not(containsString("value=\"BBJ-0104\""))));
	}

	@Test
	void losCamposObligatoriosVaciosNoSeGuardan() throws Exception {
		mvc.perform(post("/equipos").with(csrf()).with(como(administrador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Revisa 7 campos")))
			.andExpect(content().string(containsString("Escribe el número de inventario.")))
			.andExpect(content().string(containsString("Escribe el número de serie.")))
			.andExpect(content().string(containsString("Escribe el nombre del equipo.")))
			.andExpect(content().string(containsString("Elige el tipo de equipo.")))
			.andExpect(content().string(containsString("Elige la marca.")))
			.andExpect(content().string(containsString("Escribe el modelo.")))
			.andExpect(content().string(containsString("Elige el área donde está el equipo.")));

		assertThat(equipos.count()).isZero();
	}

	@Test
	void losDatosInvalidosNoSeGuardan() throws Exception {
		mvc.perform(computadora("BBJ-0105", "SERIE-0105", "memoriaGb", "0", "costo", "-1", "fechaAdquisicion",
				LocalDate.now().plusDays(1).toString()))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("La memoria debe ser de al menos 1 GB.")))
			.andExpect(content().string(containsString("El costo no puede ser negativo.")))
			.andExpect(content().string(containsString("La fecha de adquisición no puede ser futura.")));

		mvc.perform(computadora("BBJ-0105", "SERIE-0105", "tipoId", tipo("Switch").toString(), "direccionIp",
				"192.168.300.1"))
			.andExpect(content().string(containsString("Escribe una dirección IP válida")));

		mvc.perform(computadora("BBJ-0105", "SERIE-0105", "memoriaGb", "mucha"))
			.andExpect(content().string(containsString("Elige un valor válido.")));

		assertThat(equipos.count()).isZero();
	}

	@Test
	void elNumeroDeInventarioYElDeSerieSonUnicos() throws Exception {
		mvc.perform(computadora("BBJ-0106", "SERIE-0106"));

		mvc.perform(computadora("bbj-0106", "OTRA-SERIE"))
			.andExpect(content().string(containsString("Ya existe un equipo con este número de inventario.")));
		mvc.perform(computadora("BBJ-0107", "serie-0106"))
			.andExpect(content().string(containsString("Ya existe un equipo con este número de serie.")));
		// El duplicado se avisa junto con los demás errores, no hasta corregirlos.
		mvc.perform(post("/equipos").with(csrf()).with(como(administrador)).param("numInventario", "BBJ-0106"))
			.andExpect(content().string(containsString("Revisa 7 campos")))
			.andExpect(content().string(containsString("Ya existe un equipo con este número de inventario.")));

		assertThat(equipos.count()).isEqualTo(1);
	}

	@Test
	void unCatalogoInexistenteSeRechaza() throws Exception {
		mvc.perform(computadora("BBJ-0108", "SERIE-0108", "marcaId", "999999"))
			.andExpect(content().string(containsString("Elige una marca de la lista.")));

		assertThat(equipos.count()).isZero();
	}

	@Test
	void soloSeGuardanLasCaracteristicasDelTipoElegido() throws Exception {
		mvc.perform(computadora("BBJ-0109", "SERIE-0109", "tipoId", tipo("Impresora").toString(), "tecnologiaImpresion",
				"Láser", "conexion", "Red"));

		Equipo impresora = equipos.findByNumInventario("BBJ-0109").orElseThrow();
		assertThat(impresora.getCaracteristicas()).containsEntry("tecnologia", "Láser")
			.containsEntry("conexion", "Red")
			.doesNotContainKey("procesador")
			.doesNotContainKey("memoriaGb");
		assertThat(impresora.getSistemaOperativo()).isNull();
	}

	@Test
	void soloElAdministradorRegistra() throws Exception {
		for (String rol : new String[] { Rol.EJECUTIVO, Rol.CONSULTA }) {
			Usuario usuario = crearUsuario(rol);
			mvc.perform(get("/equipos/nuevo").with(como(usuario))).andExpect(status().isForbidden());
			mvc.perform(post("/equipos").with(csrf())
				.with(como(usuario))
				.param("numInventario", "BBJ-0110")
				.param("numSerie", "SERIE-0110")
				.param("nombre", "PC")
				.param("tipoId", tipo("Laptop").toString())
				.param("marcaId", marca("Dell").toString())
				.param("modelo", "Latitude")
				.param("areaId", sala.getId().toString())).andExpect(status().isForbidden());
			mvc.perform(get("/equipos").with(como(usuario)))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString("Registrar equipo"))));
		}

		assertThat(equipos.count()).isZero();
	}

	@Test
	void sinAreasElFormularioExplicaQueFaltan() throws Exception {
		areas.deleteAll();

		mvc.perform(get("/equipos/nuevo").with(como(administrador)))
			.andExpect(content().string(containsString("Primero hace falta registrar las áreas")));
	}

	@Test
	void laBaseDeDatosRechazaDuplicadosYCostosNegativosAunqueNoPasenPorElFormulario() {
		jdbc.update("""
				insert into equipos (num_inventario, num_serie, nombre, tipo_equipo_id, marca_id, modelo, area_id, estado,
				qr_uuid, created_at, updated_at) values ('BBJ-0111', 'S-1', 'PC', ?, ?, 'M', ?, 'operativo', uuid(), now(), now())
				""", tipo("Laptop"), marca("Dell"), sala.getId());

		assertThatThrownBy(() -> jdbc.update("""
				insert into equipos (num_inventario, num_serie, nombre, tipo_equipo_id, marca_id, modelo, area_id, estado,
				qr_uuid, created_at, updated_at) values ('BBJ-0111', 'S-2', 'PC', ?, ?, 'M', ?, 'operativo', uuid(), now(), now())
				""", tipo("Laptop"), marca("Dell"), sala.getId())).isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbc.update("""
				insert into equipos (num_inventario, num_serie, nombre, tipo_equipo_id, marca_id, modelo, area_id, estado,
				qr_uuid, costo, created_at, updated_at) values ('BBJ-0112', 'S-3', 'PC', ?, ?, 'M', ?, 'operativo', uuid(), -5, now(), now())
				""", tipo("Laptop"), marca("Dell"), sala.getId())).isInstanceOf(DataAccessException.class)
			.hasMessageContaining("ck_equipos_costo");
		assertThatThrownBy(() -> jdbc.update("""
				insert into equipos (num_inventario, num_serie, nombre, tipo_equipo_id, marca_id, modelo, area_id, estado,
				qr_uuid, created_at, updated_at) values ('BBJ-0113', 'S-4', 'PC', ?, ?, 'M', ?, 'perdido', uuid(), now(), now())
				""", tipo("Laptop"), marca("Dell"), sala.getId())).isInstanceOf(DataAccessException.class)
			.hasMessageContaining("ck_equipos_estado");
	}

}
