package mx.tecnologiccorp.sigit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.TimeZone;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;

import mx.tecnologiccorp.sigit.catalogos.Area;
import mx.tecnologiccorp.sigit.catalogos.MarcaRepository;
import mx.tecnologiccorp.sigit.catalogos.TipoEquipoRepository;
import mx.tecnologiccorp.sigit.config.ZonaHoraria;
import mx.tecnologiccorp.sigit.inventario.Equipo;
import mx.tecnologiccorp.sigit.inventario.EquipoForm;
import mx.tecnologiccorp.sigit.inventario.EquipoService;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.Usuario;

/**
 * Tarea 6: regresión de los defectos D-01 a D-08 que encontró la validación del módulo de captura de
 * inventario. D-01, D-03, D-04, D-05, D-06 y D-08 fallan con el código anterior a su corrección; D-02 (clase
 * nueva) y D-07 (solo se veía con devtools, en el perfil dev) se comprobaron además con la aplicación
 * corriendo.
 */
class ValidacionDelModuloTest extends PruebaIntegracion {

	@Autowired
	private EquipoService servicio;

	@Autowired
	private TipoEquipoRepository tipos;

	@Autowired
	private MarcaRepository marcas;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private Environment entorno;

	private Usuario administrador;

	private Area sala;

	@BeforeEach
	void datos() {
		administrador = crearUsuario(Rol.ADMINISTRADOR, "admin.validacion@sigit.test");
		sala = crearArea("Sala de consulta");
	}

	private Equipo registrar(String inventario, Area area) {
		EquipoForm f = new EquipoForm();
		f.setNumInventario(inventario);
		f.setNumSerie("S-" + inventario);
		f.setNombre("Equipo " + inventario);
		f.setTipoId(tipos.findAll().getFirst().getId());
		f.setMarcaId(marcas.findAll().getFirst().getId());
		f.setModelo("Modelo");
		f.setAreaId(area.getId());
		f.setFechaAdquisicion(LocalDate.of(2025, 1, 1));
		return servicio.registrar(f, administrador.getId());
	}

	@Test
	void d01LaEtiquetaDeUnNumeroYUnAreaLargosLosConserva() throws Exception {
		Area larga = areas.save(new Area("Coordinación de Servicios Bibliotecarios", "Edificio principal", "3"));
		Equipo equipo = registrar("BBJ-INVENTARIO-PATRIMONIAL-01", larga);

		byte[] pdf = mvc.perform(get("/equipos/" + equipo.getId() + "/etiqueta.pdf").with(como(administrador)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsByteArray();

		String texto = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1).replaceAll("\\s", "");
		assertThat(texto).contains("BBJ-INVENTARIO-PATRIMONIAL-01", "Coordinación");
	}

	@Test
	void d02LaZonaHorariaDeSigitNoDependeDelServidor() {
		TimeZone anterior = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
			MockEnvironment entorno = new MockEnvironment().withProperty(ZonaHoraria.PROPIEDAD, "America/Merida");
			new ZonaHoraria().onApplicationEvent(new ApplicationEnvironmentPreparedEvent(null, new SpringApplication(),
					new String[0], entorno));
			assertThat(TimeZone.getDefault().getID()).isEqualTo("America/Merida");

			new ZonaHoraria().onApplicationEvent(
					new ApplicationEnvironmentPreparedEvent(null, new SpringApplication(), new String[0],
							new StandardEnvironment()));
			assertThat(TimeZone.getDefault().getID()).as("sin la propiedad, Mérida por omisión")
				.isEqualTo("America/Merida");
		}
		finally {
			TimeZone.setDefault(anterior);
		}
	}

	@Test
	void d03LasPaginasLlevanPoliticaDeContenidoYDeReferencia() throws Exception {
		for (String ruta : new String[] { "/login", "/equipos" }) {
			mvc.perform(get(ruta).with(como(administrador)))
				.andExpect(header().string("Content-Security-Policy", Matchers.containsString("default-src 'self'")))
				.andExpect(header().string("Content-Security-Policy", Matchers.containsString("frame-ancestors 'none'")))
				.andExpect(header().string("Referrer-Policy", "same-origin"))
				.andExpect(header().string("Permissions-Policy", Matchers.containsString("camera=(self)")));
		}
	}

	@Test
	void d04UnaPaginaQueYaNoExisteLlevaALaUltima() throws Exception {
		for (int i = 1; i <= 17; i++) {
			registrar("BBJ-%04d".formatted(i), sala);
		}

		// La dirección completa, como la manda el navegador: la redirección conserva los demás filtros.
		mvc.perform(get("/equipos?pagina=999&orden=nombre").with(como(administrador)))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", Matchers.allOf(Matchers.containsString("pagina=2"),
					Matchers.containsString("orden=nombre"))));
	}

	@Test
	void d05MasDeDoscientasCuarentaEtiquetasSeRechazanConAviso() throws Exception {
		String[] ids = new String[241];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = String.valueOf(1_000_000 + i);
		}

		mvc.perform(get("/equipos/etiquetas").param("id", ids).with(como(administrador)))
			.andExpect(redirectedUrl("/equipos"))
			.andExpect(flash().attribute("error", Matchers.containsString("hasta 240 etiquetas")));
	}

	@Test
	void d06UnIdentificadorQueNoEsNumeroRespondeNoEncontrado() throws Exception {
		for (String ruta : new String[] { "/equipos/abc", "/equipos/abc/editar", "/equipos/abc/qr.png",
				"/equipos/99999999999999999999" }) {
			mvc.perform(get(ruta).accept(MediaType.TEXT_HTML).with(como(administrador)))
				.andExpect(status().isNotFound());
		}
	}

	@Test
	void d07LosErroresNoMuestranTrazaNiMensajeInterno() throws Exception {
		Equipo equipo = registrar("BBJ-0001", sala);
		// D-09: con el nombre viejo (server.error.*) Spring Boot 4 ignora la configuración.
		assertThat(entorno.getProperty("spring.web.error.include-stacktrace")).isEqualTo("never");
		assertThat(entorno.getProperty("spring.web.error.include-message")).isEqualTo("never");
		assertThat(entorno.getProperty("spring.web.error.whitelabel.enabled")).isEqualTo("false");

		mvc.perform(get("/equipos/999999").accept(MediaType.APPLICATION_JSON).with(como(administrador)))
			.andExpect(status().isNotFound())
			.andExpect(content().string(Matchers.not(Matchers.containsString("trace"))))
			.andExpect(content().string(Matchers.not(Matchers.containsString("EquipoNoEncontrado"))));
		assertThat(equipo.getId()).isNotNull();
	}

	@Test
	void d08UnaVersionQueNoEsNumeroNoSeGuardaYAvisa() throws Exception {
		Equipo equipo = registrar("BBJ-0001", sala);
		jdbc.update("update equipos set estado = 'candidato_baja' where id = ?", equipo.getId());

		mvc.perform(post("/equipos/" + equipo.getId()).with(csrf()).with(como(administrador))
			.param("version", "abc").param("nombre", "Otro"))
			.andExpect(status().isOk())
			.andExpect(content().string(Matchers.containsString("Alguien más guardó cambios")));
		mvc.perform(post("/equipos/" + equipo.getId() + "/baja").with(csrf()).with(como(administrador))
			.param("version", "abc").param("motivo", "Motivo suficientemente largo.").param("confirmo", "true"))
			.andExpect(redirectedUrl("/equipos/" + equipo.getId()))
			.andExpect(flash().attribute("error", Matchers.containsString("Alguien más modificó este equipo")));

		assertThat(jdbc.queryForObject("select estado from equipos where id = ?", String.class, equipo.getId()))
			.isEqualTo("candidato_baja");
		assertThat(jdbc.queryForObject("select nombre from equipos where id = ?", String.class, equipo.getId()))
			.isEqualTo("Equipo BBJ-0001");
	}

}
