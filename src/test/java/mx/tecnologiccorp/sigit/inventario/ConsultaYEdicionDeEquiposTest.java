package mx.tecnologiccorp.sigit.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.HtmlUtils;

import mx.tecnologiccorp.sigit.PruebaIntegracion;
import mx.tecnologiccorp.sigit.catalogos.Area;
import mx.tecnologiccorp.sigit.catalogos.MarcaRepository;
import mx.tecnologiccorp.sigit.catalogos.TipoEquipoRepository;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.Usuario;

/** Tarea 3: consulta (listado, búsqueda y ficha) y edición de equipos guardando en MySQL 8.4. */
class ConsultaYEdicionDeEquiposTest extends PruebaIntegracion {

	@Autowired
	private EquipoService servicio;

	@Autowired
	private TipoEquipoRepository tipos;

	@Autowired
	private MarcaRepository marcas;

	@Autowired
	private JdbcTemplate jdbc;

	private Usuario administrador;

	private Area sala;

	private Area hemeroteca;

	@BeforeEach
	void datos() {
		administrador = crearUsuario(Rol.ADMINISTRADOR, "admin.consulta@sigit.test");
		sala = crearArea("Sala de consulta");
		hemeroteca = crearArea("Hemeroteca");
	}

	private Long tipo(String nombre) {
		return tipos.findAll().stream().filter(t -> t.getNombre().equals(nombre)).findFirst().orElseThrow().getId();
	}

	private Long marca(String nombre) {
		return marcas.findAll().stream().filter(m -> m.getNombre().equals(nombre)).findFirst().orElseThrow().getId();
	}

	/** Datos completos de una computadora; cada par de {@code cambios} (campo, valor) reemplaza uno. */
	private Map<String, String> datos(String inventario, String serie, String... cambios) {
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
		return datos;
	}

	/** Registra el equipo con el mismo servicio que usa el alta (Tarea 2). */
	private Equipo registrar(String inventario, String serie, String... cambios) {
		EquipoForm formulario = new EquipoForm();
		datos(inventario, serie, cambios).forEach((campo, valor) -> asignar(formulario, campo, valor));
		return servicio.registrar(formulario);
	}

	private static void asignar(EquipoForm f, String campo, String valor) {
		switch (campo) {
			case "numInventario" -> f.setNumInventario(valor);
			case "numSerie" -> f.setNumSerie(valor);
			case "nombre" -> f.setNombre(valor);
			case "tipoId" -> f.setTipoId(Long.valueOf(valor));
			case "marcaId" -> f.setMarcaId(Long.valueOf(valor));
			case "modelo" -> f.setModelo(valor);
			case "areaId" -> f.setAreaId(Long.valueOf(valor));
			case "procesador" -> f.setProcesador(valor);
			case "memoriaGb" -> f.setMemoriaGb(Integer.valueOf(valor));
			case "almacenamientoGb" -> f.setAlmacenamientoGb(Integer.valueOf(valor));
			case "tipoAlmacenamiento" -> f.setTipoAlmacenamiento(valor);
			case "sistemaOperativo" -> f.setSistemaOperativo(valor);
			case "fechaAdquisicion" -> f.setFechaAdquisicion(LocalDate.parse(valor));
			case "costo" -> f.setCosto(new BigDecimal(valor));
			case "observaciones" -> f.setObservaciones(valor);
			default -> throw new IllegalArgumentException(campo);
		}
	}

	/** Envía la edición con los datos de {@link #datos} y la versión que se abrió. */
	private MockHttpServletRequestBuilder editar(Equipo equipo, Usuario quien, String... cambios) {
		MockHttpServletRequestBuilder peticion = post("/equipos/" + equipo.getId()).with(csrf()).with(como(quien));
		Map<String, String> valores = datos(equipo.getNumInventario(), equipo.getNumSerie());
		valores.put("version", String.valueOf(equipo.getVersion()));
		for (int i = 0; i < cambios.length; i += 2) {
			valores.put(cambios[i], cambios[i + 1]);
		}
		valores.forEach(peticion::param);
		return peticion;
	}

	private String pagina(String url, Usuario quien) throws Exception {
		return mvc.perform(get(url).with(como(quien))).andExpect(status().isOk()).andReturn().getResponse()
			.getContentAsString();
	}

	private Equipo recargar(Equipo equipo) {
		return equipos.findById(equipo.getId()).orElseThrow();
	}

	// ---------- Consulta ----------

	@Test
	void elListadoMuestraLaInformacionPrincipalDeCadaEquipo() throws Exception {
		registrar("BBJ-0201", "MXL0201");
		registrar("BBJ-0202", "SW0202", "nombre", "Switch del site", "tipoId", tipo("Switch").toString(), "marcaId",
				marca("Cisco").toString(), "modelo", "CBS250-24T", "areaId", hemeroteca.getId().toString());

		String html = pagina("/equipos", crearUsuario(Rol.EJECUTIVO));

		assertThat(html).contains("Mostrando 1 a 2 de 2 equipos", "BBJ-0201", "MXL0201", "PC de préstamo 3",
				"Computadora de escritorio", "HP", "ProDesk 405 G8", "Sala de consulta", "Ryzen 5 5600G · 16 GB · Windows 11",
				"BBJ-0202", "Switch del site", "Cisco", "Hemeroteca", "Operativo");
		assertThat(html.indexOf("BBJ-0201")).isLessThan(html.indexOf("BBJ-0202"));
	}

	@ParameterizedTest(name = "«{0}» encuentra {1}")
	@CsvSource({ "bbj-0201, BBJ-0201", "0202, BBJ-0202", "mxl02, BBJ-0201", "prestamo, BBJ-0201",
			"Switch del, BBJ-0202", "CBS250, BBJ-0202" })
	void seLocalizaUnEquipoPorSusDatosDeIdentificacion(String buscado, String esperado) throws Exception {
		registrar("BBJ-0201", "MXL0201");
		registrar("BBJ-0202", "SW0202", "nombre", "Switch del site", "tipoId", tipo("Switch").toString(), "modelo",
				"CBS250-24T");
		String otro = esperado.equals("BBJ-0201") ? "BBJ-0202" : "BBJ-0201";

		mvc.perform(get("/equipos").param("q", buscado).with(como(administrador)))
			.andExpect(content().string(allOf(containsString("Se encontró 1 equipo"), containsString(esperado),
					not(containsString(">" + otro + "<")))));
	}

	@Test
	void unCodigoQrLeidoLocalizaSuEquipo() throws Exception {
		Equipo equipo = registrar("BBJ-0201", "MXL0201");
		registrar("BBJ-0202", "MXL0202");

		mvc.perform(get("/equipos").param("q", equipo.getQrUuid().toUpperCase()).with(como(administrador)))
			.andExpect(content().string(allOf(containsString("Se encontró 1 equipo"), containsString("BBJ-0201"))));
	}

	@Test
	void losComodinesSeBuscanComoTexto() throws Exception {
		registrar("BBJ-0201", "MXL0201");

		mvc.perform(get("/equipos").param("q", "%").with(como(administrador)))
			.andExpect(content().string(containsString("Ningún equipo coincide con la búsqueda")));
		mvc.perform(get("/equipos").param("q", "BBJ_0201").with(como(administrador)))
			.andExpect(content().string(containsString("Ningún equipo coincide con la búsqueda")));
	}

	@Test
	void losFiltrosPorAreaTipoYEstadoSeAplicanEnLaBaseDeDatos() throws Exception {
		registrar("BBJ-0201", "S1");
		registrar("BBJ-0202", "S2", "areaId", hemeroteca.getId().toString());
		Equipo laptop = registrar("BBJ-0203", "S3", "tipoId", tipo("Laptop").toString());
		jdbc.update("update equipos set estado = 'en_reparacion' where id = ?", laptop.getId());

		assertThat(pagina("/equipos?area=" + hemeroteca.getId(), administrador)).contains("BBJ-0202")
			.doesNotContain(">BBJ-0201<", ">BBJ-0203<");
		assertThat(pagina("/equipos?tipo=" + tipo("Laptop"), administrador)).contains("BBJ-0203")
			.doesNotContain(">BBJ-0201<", ">BBJ-0202<");
		assertThat(pagina("/equipos?estado=EN_REPARACION", administrador)).contains("BBJ-0203", "En reparación")
			.doesNotContain(">BBJ-0201<");
	}

	@Test
	void elListadoSeOrdenaYSePaginaDe15En15() throws Exception {
		for (int i = 1; i <= 17; i++) {
			registrar("BBJ-%04d".formatted(i), "S-%04d".formatted(i), "nombre", "Equipo %02d".formatted(18 - i));
		}

		assertThat(pagina("/equipos", administrador)).contains("Mostrando 1 a 15 de 17 equipos", "BBJ-0001")
			.doesNotContain(">BBJ-0016<");
		assertThat(pagina("/equipos?pagina=2", administrador)).contains("Mostrando 16 a 17 de 17 equipos", "BBJ-0017");
		// Por nombre, «Equipo 01» (BBJ-0017) va primero y «Equipo 17» (BBJ-0001) pasa a la página 2.
		String porNombre = pagina("/equipos?orden=nombre", administrador);
		assertThat(porNombre.indexOf(">BBJ-0017<")).isLessThan(porNombre.indexOf(">BBJ-0016<"));
		assertThat(porNombre).doesNotContain(">BBJ-0001<");
	}

	@Test
	void consultaSoloVeLosEquiposDeSuArea() throws Exception {
		Usuario consulta = crearUsuario(Rol.CONSULTA);
		Equipo propio = registrar("BBJ-0201", "S1", "areaId", consulta.getArea().getId().toString());
		Equipo ajeno = registrar("BBJ-0202", "S2");

		// Aunque pida otra área en la dirección, el filtro queda fijo en la suya.
		assertThat(pagina("/equipos?area=" + sala.getId(), consulta)).contains("BBJ-0201", "el área asignada a tu usuario")
			.doesNotContain(">BBJ-0202<", "Nuevo equipo", "/editar");
		mvc.perform(get("/equipos/" + propio.getId()).with(como(consulta))).andExpect(status().isOk());
		mvc.perform(get("/equipos/" + ajeno.getId()).with(como(consulta))).andExpect(status().isNotFound());
	}

	@Test
	void laFichaMuestraTodaLaInformacionDelEquipo() throws Exception {
		Equipo equipo = registrar("BBJ-0201", "MXL0201");

		assertThat(pagina("/equipos/" + equipo.getId(), crearUsuario(Rol.EJECUTIVO))).contains("Equipo BBJ-0201",
				"PC de préstamo 3", "Computadora de escritorio", "HP", "ProDesk 405 G8", "MXL0201", "Operativo",
				"Sala de consulta (Edificio principal, piso 1)", "Ryzen 5 5600G", "16 GB", "512 GB SSD", "Windows 11",
				"20/10/2026", "$15,899.50 MXN", "Llegó con teclado y mouse.")
			.doesNotContain("/equipos/" + equipo.getId() + "/editar");
		assertThat(pagina("/equipos/" + equipo.getId(), administrador))
			.contains("/equipos/" + equipo.getId() + "/editar");
	}

	@Test
	void unEquipoQueNoExisteResponde404() throws Exception {
		mvc.perform(get("/equipos/999999").with(como(administrador))).andExpect(status().isNotFound());
		mvc.perform(get("/equipos/999999/editar").with(como(administrador))).andExpect(status().isNotFound());
	}

	// ---------- Edición ----------

	@Test
	void elFormularioDeEdicionTraeLosDatosGuardados() throws Exception {
		Equipo equipo = registrar("BBJ-0201", "MXL0201");

		String html = HtmlUtils.htmlUnescape(pagina("/equipos/" + equipo.getId() + "/editar", administrador));

		assertThat(html).contains("Editar equipo BBJ-0201",
				"value=\"BBJ-0201\"", "value=\"PC de préstamo 3\"", "value=\"Ryzen 5 5600G\"", "value=\"16\"",
				"value=\"512\"", "value=\"SSD\" selected=\"selected\"", "value=\"2026-10-20\"", "value=\"15899.50\"",
				"Llegó con teclado y mouse.", "name=\"version\" value=\"0\"", "Guardar cambios")
			.doesNotContain("Guardar y agregar otro");
	}

	@Test
	void soloElAdministradorEdita() throws Exception {
		Equipo equipo = registrar("BBJ-0201", "MXL0201");
		for (String rol : new String[] { Rol.EJECUTIVO, Rol.CONSULTA }) {
			Usuario usuario = crearUsuario(rol);
			mvc.perform(get("/equipos/" + equipo.getId() + "/editar").with(como(usuario)))
				.andExpect(status().isForbidden());
			mvc.perform(editar(equipo, usuario, "nombre", "Cambiado")).andExpect(status().isForbidden());
		}

		assertThat(recargar(equipo).getNombre()).isEqualTo("PC de préstamo 3");
	}

	@Test
	void losCambiosSeGuardanEnLaBaseDeDatos() throws Exception {
		Equipo equipo = registrar("BBJ-0201", "MXL0201");

		MvcResult resultado = mvc
			.perform(editar(equipo, administrador, "nombre", "PC de préstamo 4", "areaId",
					hemeroteca.getId().toString(), "memoriaGb", "32", "costo", "16000"))
			.andExpect(redirectedUrl("/equipos/" + equipo.getId()))
			.andReturn();

		EquipoActualizado actualizado = (EquipoActualizado) resultado.getFlashMap().get("actualizado");
		assertThat(actualizado.cambios()).containsExactly("Nombre", "Área", "Memoria RAM", "Costo");
		Equipo guardado = recargar(equipo);
		assertThat(guardado.getNombre()).isEqualTo("PC de préstamo 4");
		assertThat(guardado.getArea().getId()).isEqualTo(hemeroteca.getId());
		assertThat(guardado.getCaracteristicas()).containsEntry("memoriaGb", 32);
		assertThat(guardado.getCosto()).isEqualByComparingTo("16000");
		assertThat(guardado.getVersion()).isEqualTo(1);
		// Lo que no se edita aquí se conserva.
		assertThat(guardado.getEstado()).isEqualTo(EstadoEquipo.OPERATIVO);
		assertThat(guardado.getQrUuid()).isEqualTo(equipo.getQrUuid());
		assertThat(jdbc.queryForObject("select nombre from equipos where id = ?", String.class, equipo.getId()))
			.isEqualTo("PC de préstamo 4");
	}

	@Test
	void laInformacionActualizadaSeReflejaAlConsultarNuevamente() throws Exception {
		Equipo equipo = registrar("BBJ-0201", "MXL0201");
		MvcResult resultado = mvc.perform(editar(equipo, administrador, "nombre", "PC de catalogación",
				"sistemaOperativo", "Ubuntu 24.04")).andReturn();
		MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession();

		// Justo después de guardar: la ficha confirma y marca lo que cambió.
		mvc.perform(get("/equipos/" + equipo.getId()).session(sesion).with(como(administrador)))
			.andExpect(content().string(allOf(containsString("Se guardaron los cambios del equipo BBJ-0201"),
					containsString("Nombre, Sistema operativo"), containsString("PC de catalogación"),
					containsString("Ubuntu 24.04"), containsString("Actualizado"))));
		// Cualquier consulta posterior, de cualquier rol, ya ve los datos nuevos.
		Usuario ejecutivo = crearUsuario(Rol.EJECUTIVO);
		assertThat(pagina("/equipos/" + equipo.getId(), ejecutivo)).contains("PC de catalogación", "Ubuntu 24.04")
			.doesNotContain("Windows 11", "Se guardaron los cambios");
		assertThat(pagina("/equipos?q=catalogacion", ejecutivo)).contains("Se encontró 1 equipo", "PC de catalogación",
				"Ryzen 5 5600G · 16 GB · Ubuntu 24.04");
	}

	@Test
	void guardarSinCambiosNoModificaElEquipo() throws Exception {
		Equipo equipo = recargar(registrar("BBJ-0201", "MXL0201"));

		MvcResult resultado = mvc.perform(editar(equipo, administrador)).andReturn();

		assertThat(((EquipoActualizado) resultado.getFlashMap().get("actualizado")).sinCambios()).isTrue();
		assertThat(recargar(equipo).getVersion()).isZero();
		assertThat(recargar(equipo).getUpdatedAt()).isEqualTo(equipo.getUpdatedAt());
	}

	@Test
	void laEdicionValidaLosMismosDatosQueElAlta() throws Exception {
		Equipo equipo = registrar("BBJ-0201", "MXL0201");

		mvc.perform(editar(equipo, administrador, "nombre", "", "costo", "-1", "memoriaGb", "0"))
			.andExpect(status().isOk())
			.andExpect(content().string(allOf(containsString("Revisa 3 campos"),
					containsString("Escribe el nombre del equipo."), containsString("El costo no puede ser negativo."),
					containsString("La memoria debe ser de al menos 1 GB."))));

		assertThat(recargar(equipo).getNombre()).isEqualTo("PC de préstamo 3");
		assertThat(recargar(equipo).getVersion()).isZero();
	}

	@Test
	void losNumerosNoPuedenRepetirseConOtroEquipoPeroSiConservarseLosPropios() throws Exception {
		registrar("BBJ-0201", "MXL0201");
		Equipo equipo = registrar("BBJ-0202", "MXL0202");

		mvc.perform(editar(equipo, administrador, "numInventario", "bbj-0201", "numSerie", "MXL0201"))
			.andExpect(content().string(allOf(containsString("Ya existe un equipo con este número de inventario."),
					containsString("Ya existe un equipo con este número de serie."))));
		mvc.perform(editar(equipo, administrador, "nombre", "Otro nombre"))
			.andExpect(redirectedUrl("/equipos/" + equipo.getId()));

		assertThat(recargar(equipo).getNumInventario()).isEqualTo("BBJ-0202");
		assertThat(recargar(equipo).getNombre()).isEqualTo("Otro nombre");
	}

	@Test
	void alCambiarDeTipoSoloQuedanLasCaracteristicasDelNuevo() throws Exception {
		Equipo equipo = registrar("BBJ-0201", "MXL0201");

		mvc.perform(editar(equipo, administrador, "tipoId", tipo("Switch").toString(), "puertos", "24",
				"direccionIp", "192.168.10.2", "administrable", "true"))
			.andExpect(redirectedUrl("/equipos/" + equipo.getId()));

		Equipo guardado = recargar(equipo);
		assertThat(guardado.getCaracteristicas()).containsOnlyKeys("puertos", "administrable", "direccionIp");
		assertThat(guardado.getSistemaOperativo()).isNull();
	}

	@Test
	void unEquipoDadoDeBajaConservaSusDatosPeroNoSeEdita() throws Exception {
		Equipo equipo = registrar("BBJ-0201", "MXL0201");
		jdbc.update("update equipos set estado = 'baja' where id = ?", equipo.getId());

		mvc.perform(get("/equipos/" + equipo.getId() + "/editar").with(como(administrador)))
			.andExpect(redirectedUrl("/equipos/" + equipo.getId()))
			.andExpect(flash().attribute("error", containsString("ya no admite cambios")));
		mvc.perform(editar(equipo, administrador, "nombre", "Cambiado"))
			.andExpect(redirectedUrl("/equipos/" + equipo.getId()));
		assertThat(pagina("/equipos/" + equipo.getId(), administrador)).contains("Baja", "PC de préstamo 3")
			.doesNotContain("/editar");

		assertThat(recargar(equipo).getNombre()).isEqualTo("PC de préstamo 3");
	}

	@Test
	void siAlguienMasGuardoPrimeroNoSeSobrescribenSusCambios() throws Exception {
		Equipo equipo = registrar("BBJ-0201", "MXL0201");
		Usuario otroAdministrador = crearUsuario(Rol.ADMINISTRADOR);

		// Las dos personas abrieron el formulario con la versión 0; la primera guarda.
		mvc.perform(editar(equipo, otroAdministrador, "nombre", "Nombre de la primera"))
			.andExpect(redirectedUrl("/equipos/" + equipo.getId()));
		mvc.perform(editar(equipo, administrador, "nombre", "Nombre de la segunda"))
			.andExpect(status().isOk())
			.andExpect(content().string(allOf(containsString("Alguien más guardó cambios en este equipo"),
					containsString("value=\"Nombre de la primera\""), containsString("name=\"version\" value=\"1\""))));

		assertThat(recargar(equipo).getNombre()).isEqualTo("Nombre de la primera");
	}

	@Test
	void sinVersionNoSeGuarda() throws Exception {
		Equipo equipo = registrar("BBJ-0201", "MXL0201");

		mvc.perform(post("/equipos/" + equipo.getId()).with(csrf())
			.with(como(administrador))
			.params(MultiValueMap.fromSingleValue(datos("BBJ-0201", "MXL0201", "nombre", "X"))))
			.andExpect(content().string(containsString("Alguien más guardó cambios")));

		assertThat(recargar(equipo).getNombre()).isEqualTo("PC de préstamo 3");
		assertThat(recargar(equipo).getVersion()).isZero();
	}

}
