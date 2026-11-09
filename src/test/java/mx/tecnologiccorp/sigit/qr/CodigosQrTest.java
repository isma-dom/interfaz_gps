package mx.tecnologiccorp.sigit.qr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import mx.tecnologiccorp.sigit.PruebaIntegracion;
import mx.tecnologiccorp.sigit.catalogos.Area;
import mx.tecnologiccorp.sigit.catalogos.MarcaRepository;
import mx.tecnologiccorp.sigit.catalogos.TipoEquipoRepository;
import mx.tecnologiccorp.sigit.inventario.Equipo;
import mx.tecnologiccorp.sigit.inventario.EquipoForm;
import mx.tecnologiccorp.sigit.inventario.EquipoService;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.Usuario;

/** Tarea 5: generación de códigos QR (RF-05, RN-05, C-13, C-14, C-26). */
class CodigosQrTest extends PruebaIntegracion {

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

	@BeforeEach
	void datos() {
		administrador = crearUsuario(Rol.ADMINISTRADOR, "admin.qr@sigit.test");
		sala = crearArea("Sala de consulta");
	}

	private Equipo registrar(String inventario, Area area) {
		EquipoForm f = new EquipoForm();
		f.setNumInventario(inventario);
		f.setNumSerie("S-" + inventario);
		f.setNombre("PC " + inventario);
		f.setTipoId(tipos.findAll().stream().filter(t -> t.getNombre().equals("Laptop")).findFirst().orElseThrow()
			.getId());
		f.setMarcaId(marcas.findAll().stream().filter(m -> m.getNombre().equals("Dell")).findFirst().orElseThrow()
			.getId());
		f.setModelo("Latitude 3440");
		f.setAreaId(area.getId());
		f.setSistemaOperativo("Windows 11");
		f.setFechaAdquisicion(LocalDate.of(2025, 6, 1));
		return servicio.registrar(f, administrador.getId());
	}

	/** Lo que lee la cámara de un celular: se descarga el PNG del sistema y se decodifica. */
	private String escanear(Equipo equipo, Usuario quien) throws Exception {
		byte[] png = mvc.perform(get("/equipos/" + equipo.getId() + "/qr.png").with(como(quien)))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "image/png"))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();
		BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));
		// La imagen es el código limpio, sin fondo de foto: PURE_BARCODE lo lee sin buscar los patrones.
		return new QRCodeReader()
			.decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(imagen))),
					Map.of(DecodeHintType.PURE_BARCODE, Boolean.TRUE, DecodeHintType.TRY_HARDER, Boolean.TRUE))
			.getText();
	}

	/** La ruta que abre el celular, sin el servidor: /e/<uuid>. */
	private static String ruta(String contenido) {
		return contenido.substring(contenido.indexOf("/e/"));
	}

	// ---------- Un código único por equipo, ligado a su registro ----------

	@Test
	void cadaEquipoRecibeUnCodigoUnicoAlRegistrarse() {
		Set<String> codigos = new HashSet<>();
		for (int i = 1; i <= 20; i++) {
			Equipo equipo = registrar("BBJ-%04d".formatted(500 + i), sala);
			assertThat(equipo.getQrUuid()).matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
			codigos.add(equipo.getQrUuid());
		}
		assertThat(codigos).hasSize(20);
		assertThat(jdbc.queryForObject("select count(distinct qr_uuid) from equipos", Integer.class)).isEqualTo(20);
	}

	@Test
	void laBaseDeDatosNoAceptaDosEquiposConElMismoCodigo() {
		Equipo equipo = registrar("BBJ-0501", sala);

		assertThatThrownBy(() -> jdbc.update("""
				insert into equipos (num_inventario, num_serie, nombre, tipo_equipo_id, marca_id, modelo, area_id, estado,
				qr_uuid, created_at, updated_at) select 'BBJ-0599', 'S-0599', nombre, tipo_equipo_id, marca_id, modelo,
				area_id, 'operativo', qr_uuid, now(), now() from equipos where id = ?
				""", equipo.getId())).isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("uk_equipos_qr_uuid");
	}

	@Test
	void elCodigoSoloLlevaLaUrlConElIdentificadorDelEquipo() throws Exception {
		Equipo equipo = registrar("BBJ-0501", sala);

		String contenido = escanear(equipo, administrador);

		assertThat(contenido).isEqualTo("http://localhost/e/" + equipo.getQrUuid());
		// RN-05: nunca datos del equipo.
		assertThat(contenido).doesNotContain("BBJ-0501", "S-BBJ", "PC ", "Dell", "Sala");
	}

	@Test
	void cadaCodigoCorrespondeAlEquipoCorrecto() throws Exception {
		List<Equipo> lista = List.of(registrar("BBJ-0501", sala), registrar("BBJ-0502", sala),
				registrar("BBJ-0503", crearArea("Hemeroteca")));

		for (Equipo equipo : lista) {
			String contenido = escanear(equipo, administrador);
			mvc.perform(get(ruta(contenido)).with(como(administrador)))
				.andExpect(redirectedUrl("/equipos/" + equipo.getId()));
		}
	}

	// ---------- Al escanearlo se identifica el equipo ----------

	@Test
	void sinSesionElCodigoPideIniciarlaYLuegoAbreLaFicha() throws Exception {
		Equipo equipo = registrar("BBJ-0501", sala);
		crearUsuario(Rol.EJECUTIVO, "ejecutivo.qr@sigit.test");
		String ruta = "/e/" + equipo.getQrUuid();

		MvcResult sinSesion = mvc.perform(get(ruta)).andExpect(redirectedUrl("/login")).andReturn();
		MockHttpSession sesion = (MockHttpSession) sinSesion.getRequest().getSession();

		mvc.perform(post("/login").session(sesion).with(csrf()).param("correo", "ejecutivo.qr@sigit.test")
			.param("password", CLAVE)).andExpect(redirectedUrlPattern("**" + ruta + "*"));
		mvc.perform(get(ruta).session(sesion)).andExpect(redirectedUrl("/equipos/" + equipo.getId()));
	}

	@Test
	void laRutaDelDisenoTambienAbreLaFicha() throws Exception {
		Equipo equipo = registrar("BBJ-0501", sala);

		mvc.perform(get("/equipos/qr/" + equipo.getQrUuid().toUpperCase()).with(como(administrador)))
			.andExpect(redirectedUrl("/equipos/" + equipo.getId()));
	}

	@Test
	void unCodigoQueNoExisteOFueraDelAreaRespondeNoEncontrado() throws Exception {
		Usuario consulta = crearUsuario(Rol.CONSULTA);
		Equipo propio = registrar("BBJ-0501", consulta.getArea());
		Equipo ajeno = registrar("BBJ-0502", sala);

		mvc.perform(get("/e/00000000-0000-4000-8000-000000000000").with(como(administrador)))
			.andExpect(status().isNotFound());
		mvc.perform(get("/e/" + ajeno.getQrUuid()).with(como(consulta))).andExpect(status().isNotFound());
		mvc.perform(get("/equipos/" + ajeno.getId() + "/qr.png").with(como(consulta)))
			.andExpect(status().isNotFound());
		mvc.perform(get("/e/" + propio.getQrUuid()).with(como(consulta)))
			.andExpect(redirectedUrl("/equipos/" + propio.getId()));
	}

	@Test
	void elCodigoDeUnEquipoDadoDeBajaSigueAbriendoSuFicha() throws Exception {
		Equipo equipo = registrar("BBJ-0501", sala);
		jdbc.update("update equipos set estado = 'baja' where id = ?", equipo.getId());

		mvc.perform(get("/e/" + equipo.getQrUuid()).with(como(administrador)))
			.andExpect(redirectedUrl("/equipos/" + equipo.getId()));
	}

	// ---------- Se visualiza en el sistema ----------

	@Test
	void laFichaMuestraElCodigoQr() throws Exception {
		Equipo equipo = registrar("BBJ-0501", sala);

		String ficha = mvc.perform(get("/equipos/" + equipo.getId()).with(como(crearUsuario(Rol.EJECUTIVO))))
			.andReturn()
			.getResponse()
			.getContentAsString();

		assertThat(ficha).contains("Código QR", "/equipos/" + equipo.getId() + "/qr.svg",
				"http://localhost/e/" + equipo.getQrUuid(), equipo.getQrUuid(), "Descargar PNG", "Descargar SVG")
			.doesNotContain("etiqueta.pdf");
		String svg = mvc.perform(get("/equipos/" + equipo.getId() + "/qr.svg").with(como(administrador)))
			.andExpect(header().string("Content-Type", "image/svg+xml;charset=UTF-8"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		assertThat(svg).startsWith("<svg").containsPattern("viewBox=\"0 0 (\\d+) \\1\"")
			.contains("<title>Código QR del equipo BBJ-0501</title>");
	}

	@Test
	void elAdministradorVeLaOpcionDeImprimirLaEtiqueta() throws Exception {
		Equipo equipo = registrar("BBJ-0501", sala);

		assertThat(mvc.perform(get("/equipos/" + equipo.getId()).with(como(administrador))).andReturn().getResponse()
			.getContentAsString()).contains("/equipos/" + equipo.getId() + "/etiqueta.pdf", "Imprimir etiqueta");
	}

	// ---------- Descarga e impresión ----------

	@Test
	void elCodigoSeDescargaEnPngYSvg() throws Exception {
		Equipo equipo = registrar("BBJ-0501", sala);
		Usuario consulta = crearUsuario(Rol.CONSULTA);
		jdbc.update("update equipos set area_id = ? where id = ?", consulta.getArea().getId(), equipo.getId());

		mvc.perform(get("/equipos/" + equipo.getId() + "/qr.png").param("descargar", "1").with(como(consulta)))
			.andExpect(header().string("Content-Disposition", "attachment; filename=\"QR-BBJ-0501.png\""));
		mvc.perform(get("/equipos/" + equipo.getId() + "/qr.svg").param("descargar", "1").with(como(consulta)))
			.andExpect(header().string("Content-Disposition", "attachment; filename=\"QR-BBJ-0501.svg\""));
	}

	@Test
	void lasEtiquetasSeGeneranEnPdfConSuCodigoInventarioYArea() throws Exception {
		Equipo uno = registrar("BBJ-0501", sala);
		Equipo dos = registrar("BBJ-0502", crearArea("Hemeroteca"));
		Equipo baja = registrar("BBJ-0503", sala);
		jdbc.update("update equipos set estado = 'baja' where id = ?", baja.getId());

		byte[] pdf = mvc.perform(get("/equipos/etiquetas").param("id", dos.getId().toString(), uno.getId().toString(),
				baja.getId().toString()).with(como(administrador)))
			.andExpect(status().isOk())
			.andExpect(header().string("Content-Type", "application/pdf"))
			.andExpect(header().string("Content-Disposition", "inline; filename=\"etiquetas-qr-2026-11-09.pdf\""))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();

		assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
		PdfReader lector = new PdfReader(pdf);
		String texto = new PdfTextExtractor(lector).getTextFromPage(1);
		assertThat(lector.getNumberOfPages()).isEqualTo(1);
		assertThat(texto).contains("BBJ-0501", "BBJ-0502", "Sala de", "Hemeroteca")
			.doesNotContain("BBJ-0503");
		assertThat(texto.indexOf("BBJ-0501")).isLessThan(texto.indexOf("BBJ-0502"));
	}

	@Test
	void veinticincoEtiquetasOcupanDosHojas() throws Exception {
		String[] ids = new String[25];
		for (int i = 0; i < 25; i++) {
			ids[i] = registrar("BBJ-%04d".formatted(600 + i), sala).getId().toString();
		}

		byte[] pdf = mvc.perform(get("/equipos/etiquetas").param("id", ids).with(como(administrador)))
			.andReturn()
			.getResponse()
			.getContentAsByteArray();

		assertThat(new PdfReader(pdf).getNumberOfPages()).isEqualTo(2);
	}

	@Test
	void soloElAdministradorImprimeEtiquetas() throws Exception {
		Equipo equipo = registrar("BBJ-0501", sala);
		for (String rol : new String[] { Rol.EJECUTIVO, Rol.CONSULTA }) {
			Usuario usuario = crearUsuario(rol);
			mvc.perform(get("/equipos/etiquetas").param("id", equipo.getId().toString()).with(como(usuario)))
				.andExpect(status().isForbidden());
			mvc.perform(get("/equipos/" + equipo.getId() + "/etiqueta.pdf").with(como(usuario)))
				.andExpect(status().isForbidden());
		}
	}

	@Test
	void sinEquiposActivosSeleccionadosNoHayPdf() throws Exception {
		Equipo baja = registrar("BBJ-0501", sala);
		jdbc.update("update equipos set estado = 'baja' where id = ?", baja.getId());

		mvc.perform(get("/equipos/etiquetas").with(como(administrador)))
			.andExpect(redirectedUrl("/equipos"))
			.andExpect(flash().attribute("error", "Selecciona al menos un equipo activo para imprimir sus etiquetas."));
		mvc.perform(get("/equipos/" + baja.getId() + "/etiqueta.pdf").with(como(administrador)))
			.andExpect(redirectedUrl("/equipos/" + baja.getId()));
	}

}
