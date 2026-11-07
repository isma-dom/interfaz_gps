package mx.tecnologiccorp.sigit.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import mx.tecnologiccorp.sigit.TestcontainersConfiguration;
import mx.tecnologiccorp.sigit.catalogos.AreaRepository;

/**
 * Pruebas con Tomcat embebido real: MockMvc no despacha a /error, así que las páginas de
 * error, los recursos estáticos y las cabeceras se verifican aquí por HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ServidorRealTest {

	@LocalServerPort
	private int puerto;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private RolRepository roles;

	@Autowired
	private AreaRepository areas;

	@Autowired
	private PasswordEncoder cifrador;

	@BeforeEach
	void datos() {
		usuarios.deleteAll();
		areas.deleteAll();
		usuarios.save(new Usuario("Ejecutivo", "ejecutivo@sigit.test", cifrador.encode("Clave1234"),
				roles.findByNombre(Rol.EJECUTIVO).orElseThrow(), null));
	}

	private HttpClient cliente() {
		return HttpClient.newBuilder().cookieHandler(new CookieManager()).followRedirects(HttpClient.Redirect.NEVER)
			.build();
	}

	private HttpResponse<String> get(HttpClient cliente, String ruta, String accept) throws Exception {
		HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta));
		if (accept != null) {
			peticion.header("Accept", accept);
		}
		return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpClient conSesion() throws Exception {
		HttpClient cliente = cliente();
		String login = get(cliente, "/login", null).body();
		Matcher token = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(login);
		assertThat(token.find()).isTrue();
		String cuerpo = "correo=ejecutivo%40sigit.test&password=Clave1234&_csrf="
				+ URLEncoder.encode(token.group(1), StandardCharsets.UTF_8);
		HttpResponse<String> respuesta = cliente.send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/login"))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString(cuerpo))
					.build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(respuesta.headers().firstValue("Location")).hasValueSatisfying(l -> assertThat(l).endsWith("/tablero"));
		return cliente;
	}

	@Test
	void laPagina403EstaEnEspanolYLlevaAlInicio() throws Exception {
		HttpResponse<String> respuesta = get(conSesion(), "/usuarios", "text/html");

		assertThat(respuesta.statusCode()).isEqualTo(403);
		assertThat(respuesta.body()).contains("No tienes permiso para ver esta página")
			.contains("Ir a la página de inicio");
	}

	@Test
	void laPagina404EstaEnEspanol() throws Exception {
		HttpResponse<String> respuesta = get(conSesion(), "/no-existe", "text/html");

		assertThat(respuesta.statusCode()).isEqualTo(404);
		assertThat(respuesta.body()).contains("No encontramos esta página");
	}

	@Test
	void sinSesionLasPaginasLlevanAlLoginYJsonRecibe401() throws Exception {
		HttpResponse<String> pagina = get(cliente(), "/usuarios", "text/html");
		assertThat(pagina.statusCode()).isEqualTo(302);
		assertThat(pagina.headers().firstValue("Location")).hasValueSatisfying(l -> assertThat(l).endsWith("/login"));

		assertThat(get(cliente(), "/usuarios", "application/json").statusCode()).isEqualTo(401);
	}

	@Test
	void respondeJson403AUnaPeticionJsonSinPermiso() throws Exception {
		HttpResponse<String> respuesta = get(conSesion(), "/usuarios", "application/json");

		assertThat(respuesta.statusCode()).isEqualTo(403);
		assertThat(respuesta.body()).isEqualTo("{\"message\":\"No tienes permiso para realizar esta acción.\"}");
	}

	@Test
	void losRecursosEstaticosSeSirvenSinSesion() throws Exception {
		for (String ruta : new String[] { "/css/sigit.css", "/js/sigit.js",
				"/webjars/bootstrap/dist/css/bootstrap.min.css", "/webjars/bootstrap/dist/js/bootstrap.bundle.min.js",
				"/webjars/bootstrap-icons/font/bootstrap-icons.min.css",
				"/fuentes/atkinson-hyperlegible-latin-400-normal.woff2" }) {
			assertThat(get(cliente(), ruta, null).statusCode()).as(ruta).isEqualTo(200);
		}
	}

	@Test
	void cabecerasDeSeguridad() throws Exception {
		HttpResponse<String> respuesta = get(cliente(), "/login", null);

		assertThat(respuesta.headers().firstValue("X-Frame-Options")).hasValue("DENY");
		assertThat(respuesta.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
		assertThat(respuesta.headers().allValues("Set-Cookie")).anySatisfy(
				cookie -> assertThat(cookie).contains("HttpOnly").contains("SameSite=Lax"));
	}

}
