package mx.tecnologiccorp.sigit.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tarea 7: los colores del cliente viven en static/css/tema.css. Esta prueba lee los tres colores de entrada
 * y las mezclas derivadas del propio archivo y comprueba el contraste WCAG 2.1 AA (4.5:1) de cada texto o
 * botón que los usa. Si alguien cambia la paleta y un par deja de cumplir, la compilación falla.
 */
class ContrasteDelTemaTest {

	private static final Pattern HEX = Pattern.compile("--(sigit-[a-z]+):\\s*#([0-9a-fA-F]{6})\\s*;");

	private static final Pattern RGB = Pattern.compile("--(sigit-[a-z]+)-rgb:\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\s*;");

	private static final Pattern MEZCLA = Pattern.compile(
			"--(sigit-[a-z-]+):\\s*color-mix\\(in srgb, var\\(--(sigit-[a-z]+)\\) (\\d+)%, #(000|fff)\\);");

	private static final Map<String, int[]> COLORES = new HashMap<>();

	private static String css;

	@BeforeAll
	static void leerTema() throws IOException {
		try (InputStream entrada = ContrasteDelTemaTest.class.getResourceAsStream("/static/css/tema.css")) {
			css = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
		}
		Matcher hex = HEX.matcher(css);
		while (hex.find()) {
			int valor = Integer.parseInt(hex.group(2), 16);
			COLORES.put(hex.group(1), new int[] { valor >> 16, (valor >> 8) & 0xff, valor & 0xff });
		}
		// color-mix(in srgb, A p%, B) = A·p + B·(1 − p), canal por canal.
		Matcher mezcla = MEZCLA.matcher(css);
		while (mezcla.find()) {
			int[] base = COLORES.get(mezcla.group(2));
			double p = Integer.parseInt(mezcla.group(3)) / 100.0;
			int fondo = "fff".equals(mezcla.group(4)) ? 255 : 0;
			int[] resultado = new int[3];
			for (int i = 0; i < 3; i++) {
				resultado[i] = (int) Math.round(base[i] * p + fondo * (1 - p));
			}
			COLORES.put(mezcla.group(1), resultado);
		}
		COLORES.put("blanco", new int[] { 255, 255, 255 });
		COLORES.put("fondo", new int[] { 0xf4, 0xf6, 0xfb });
	}

	private static double luminancia(int[] color) {
		double[] c = new double[3];
		for (int i = 0; i < 3; i++) {
			double v = color[i] / 255.0;
			c[i] = v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
		}
		return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
	}

	static double contraste(int[] a, int[] b) {
		double la = luminancia(a), lb = luminancia(b);
		return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
	}

	@Test
	void elTemaDefineLosTresColoresDelCliente() {
		assertThat(COLORES).containsKeys("sigit-primario", "sigit-secundario", "sigit-acento");
	}

	@Test
	void cadaTripleteRgbCoincideConSuColor() {
		Matcher rgb = RGB.matcher(css);
		int encontrados = 0;
		while (rgb.find()) {
			encontrados++;
			assertThat(COLORES.get(rgb.group(1))).as("--%s-rgb", rgb.group(1))
				.containsExactly(Integer.parseInt(rgb.group(2)), Integer.parseInt(rgb.group(3)),
						Integer.parseInt(rgb.group(4)));
		}
		assertThat(encontrados).isEqualTo(3);
	}

	@ParameterizedTest(name = "{0} sobre {1}: {2}")
	@CsvSource({ "blanco, sigit-primario, botón de acción principal",
			"blanco, sigit-primario-oscuro, botón principal al pasar el ratón",
			"blanco, sigit-primario-mas-oscuro, botón principal presionado",
			"sigit-primario, blanco, botón secundario con borde",
			"blanco, sigit-secundario, texto del menú lateral",
			"sigit-secundario-texto, sigit-secundario, texto del menú lateral",
			"sigit-secundario-texto, sigit-secundario-claro, texto de la tarjeta del inicio de sesión",
			"blanco, sigit-secundario-claro, opción activa del menú",
			"blanco, sigit-acento, paginación activa y número de sección",
			"blanco, sigit-acento-oscuro, botón de búsqueda al pasar el ratón",
			"blanco, sigit-acento-mas-oscuro, botón de búsqueda presionado",
			"sigit-acento, blanco, enlaces",
			"sigit-acento, fondo, enlaces sobre el fondo de la página",
			"sigit-acento-oscuro, blanco, enlaces al pasar el ratón",
			"sigit-acento, sigit-acento-tenue, ícono al pasar el ratón",
			"sigit-acento-oscuro, sigit-acento-suave, paginación al pasar el ratón",
			"sigit-primario-texto, sigit-primario-claro, marca «Actualizado»" })
	void cumpleContrasteAA(String texto, String fondo, String uso) {
		assertThat(contraste(COLORES.get(texto), COLORES.get(fondo))).as(uso).isGreaterThanOrEqualTo(4.5);
	}

	@Test
	void laFormulaDeContrasteCoincideConCasosConocidos() {
		assertThat(contraste(new int[] { 0, 0, 0 }, new int[] { 255, 255, 255 })).isCloseTo(21.0,
				org.assertj.core.data.Offset.offset(0.01));
		assertThat(contraste(new int[] { 0x77, 0x77, 0x77 }, new int[] { 255, 255, 255 })).isCloseTo(4.48,
				org.assertj.core.data.Offset.offset(0.01));
	}

}
