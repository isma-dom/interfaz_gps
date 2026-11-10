package mx.tecnologiccorp.sigit.qr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.BaseFont;

/**
 * Tarea 6, defecto D-01: en la etiqueta de 5 × 3 cm, un número de inventario o un área largos se cortaban sin
 * aviso (el número quedaba incompleto y el área desaparecía). Ahora cada texto se mide antes de escribirlo.
 */
class EtiquetasPdfTest {

	/** Ancho de la columna de texto de la etiqueta, en puntos (unos 1.9 cm). */
	private static final float ANCHO = 54;

	private static final BaseFont NEGRITA = fuente(BaseFont.HELVETICA_BOLD), NORMAL = fuente(BaseFont.HELVETICA);

	private static BaseFont fuente(String nombre) {
		try {
			return BaseFont.createFont(nombre, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
		}
		catch (Exception error) {
			throw new IllegalStateException(error);
		}
	}

	private static void cabe(EtiquetasPdf.Bloque bloque, BaseFont fuente, float alto) {
		assertThat(bloque.alto()).isLessThanOrEqualTo(alto + 0.01f);
		bloque.renglones()
			.forEach(r -> assertThat(fuente.getWidthPoint(r, bloque.tamano())).isLessThanOrEqualTo(ANCHO + 0.01f));
	}

	@Test
	void unNumeroCortoVaEnUnRenglonATamanoCompleto() {
		EtiquetasPdf.Bloque bloque = EtiquetasPdf.acomodar("BBJ-0201", NEGRITA, ANCHO, 25.5f, 10.5f, 5, false);

		assertThat(bloque.renglones()).containsExactly("BBJ-0201");
		assertThat(bloque.tamano()).isEqualTo(10.5f);
	}

	@Test
	void unNumeroDeTreintaCaracteresSeImprimeCompleto() {
		String inventario = "BBJ-INVENTARIO-PATRIMONIAL-01";

		EtiquetasPdf.Bloque bloque = EtiquetasPdf.acomodar(inventario, NEGRITA, ANCHO, 25.5f, 10.5f, 5, false);

		assertThat(String.join("", bloque.renglones())).isEqualTo(inventario);
		cabe(bloque, NEGRITA, 25.5f);
	}

	@Test
	void unNumeroLargoPrefiereUnRenglonConLetraMasChica() {
		EtiquetasPdf.Bloque bloque = EtiquetasPdf.acomodar("BBJ-2026-00123", NEGRITA, ANCHO, 25.5f, 10.5f, 5, false);

		assertThat(bloque.renglones()).containsExactly("BBJ-2026-00123");
		assertThat(bloque.tamano()).isLessThan(10.5f).isGreaterThanOrEqualTo(5);
	}

	@Test
	void unAreaLargaSeParteYSiNoCabeTerminaEnPuntosSuspensivos() {
		String area = "Coordinación de Servicios Bibliotecarios y Atención al Público del Edificio Anexo";

		EtiquetasPdf.Bloque bloque = EtiquetasPdf.acomodar(area, NORMAL, ANCHO, 30, 7, 5.5f, true);

		assertThat(bloque.renglones().getLast()).endsWith("…");
		assertThat(String.join(" ", bloque.renglones())).startsWith("Coordinación de Servicios");
		cabe(bloque, NORMAL, 30);
	}

	@Test
	void unAreaQueCabeNoSeRecorta() {
		EtiquetasPdf.Bloque bloque = EtiquetasPdf.acomodar("Sala de consulta", NORMAL, ANCHO, 30, 7, 5.5f, true);

		assertThat(String.join(" ", bloque.renglones())).isEqualTo("Sala de consulta");
		assertThat(bloque.tamano()).isEqualTo(7);
	}

}
