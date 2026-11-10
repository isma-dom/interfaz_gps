package mx.tecnologiccorp.sigit.qr;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.zxing.common.BitMatrix;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.PageSize;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import mx.tecnologiccorp.sigit.inventario.Equipo;

/**
 * C-13 / C-27. Hoja tamaño carta con etiquetas de 5 × 3 cm (3 columnas por 8 filas): el código QR, el
 * número de inventario y el área de cada equipo. El QR se dibuja en vectores, módulo por módulo, para que
 * se imprima nítido; el borde gris es la guía de corte.
 */
@Component
public class EtiquetasPdf {

	private static final float CM = 72f / 2.54f;

	private static final float ANCHO = 5 * CM, ALTO = 3 * CM, SEPARACION_X = 0.3f * CM, SEPARACION_Y = 0.2f * CM;

	private static final int COLUMNAS = 3, FILAS = 8;

	private static final float LADO_QR = 2.6f * CM;

	/** Espacio para el número de inventario: hasta tres renglones de letra chica. */
	private static final float ALTO_INVENTARIO = 0.9f * CM;

	private static final float INTERLINEADO = 1.15f;

	private static final Color GRIS = new Color(0x64, 0x74, 0x8b), GUIA = new Color(0xcb, 0xd5, 0xe1);

	private final QrService qr;

	public EtiquetasPdf(QrService qr) {
		this.qr = qr;
	}

	public static int porHoja() {
		return COLUMNAS * FILAS;
	}

	public byte[] generar(List<Equipo> equipos) {
		ByteArrayOutputStream salida = new ByteArrayOutputStream();
		Document documento = new Document(PageSize.LETTER, 0, 0, 0, 0);
		PdfWriter escritor = PdfWriter.getInstance(documento, salida);
		documento.addTitle("Etiquetas QR · SIGIT");
		documento.addCreator("SIGIT, Biblioteca Benito Juárez");
		documento.open();
		BaseFont normal = fuente(BaseFont.HELVETICA), negrita = fuente(BaseFont.HELVETICA_BOLD);
		float margenX = (PageSize.LETTER.getWidth() - COLUMNAS * ANCHO - (COLUMNAS - 1) * SEPARACION_X) / 2;
		float margenY = (PageSize.LETTER.getHeight() - FILAS * ALTO - (FILAS - 1) * SEPARACION_Y) / 2;
		for (int i = 0; i < equipos.size(); i++) {
			if (i > 0 && i % porHoja() == 0) {
				documento.newPage();
			}
			int lugar = i % porHoja();
			float x = margenX + (lugar % COLUMNAS) * (ANCHO + SEPARACION_X);
			float y = PageSize.LETTER.getHeight() - margenY - (lugar / COLUMNAS + 1) * ALTO
					- (lugar / COLUMNAS) * SEPARACION_Y;
			etiqueta(escritor.getDirectContent(), equipos.get(i), x, y, normal, negrita);
		}
		documento.close();
		return salida.toByteArray();
	}

	private void etiqueta(PdfContentByte lienzo, Equipo equipo, float x, float y, BaseFont normal, BaseFont negrita) {
		// Guía de corte.
		lienzo.setColorStroke(GUIA);
		lienzo.setLineWidth(0.5f);
		lienzo.roundRectangle(x, y, ANCHO, ALTO, 4);
		lienzo.stroke();

		// Código QR, con su margen blanco incluido en la matriz.
		BitMatrix m = qr.matriz(qr.contenido(equipo));
		float modulo = LADO_QR / m.getWidth();
		float qx = x + 0.15f * CM, qy = y + (ALTO - LADO_QR) / 2;
		lienzo.setColorFill(Color.BLACK);
		for (int fila = 0; fila < m.getHeight(); fila++) {
			for (int col = 0; col < m.getWidth(); col++) {
				if (m.get(col, fila)) {
					lienzo.rectangle(qx + col * modulo, qy + LADO_QR - (fila + 1) * modulo, modulo, modulo);
				}
			}
		}
		lienzo.fill();

		// Textos a la derecha del código. Se acomodan midiendo cada renglón: nada se descarta en silencio.
		float tx = qx + LADO_QR + 0.05f * CM, ancho = x + ANCHO - 0.15f * CM - tx;
		float arriba = y + ALTO - 0.3f * CM, abajo = y + 0.55f * CM;
		escribir(lienzo, List.of("SIGIT"), negrita, 6, GRIS, tx, arriba - 5);
		float baseInventario = arriba - 8;
		Bloque inventario = acomodar(equipo.getNumInventario(), negrita, ancho, ALTO_INVENTARIO, 10.5f, 5, false);
		float siguiente = escribir(lienzo, inventario.renglones(), negrita, inventario.tamano(), Color.BLACK, tx,
				baseInventario - inventario.tamano());
		Bloque area = acomodar(equipo.getArea().getNombre(), normal, ancho, siguiente - 2 - abajo, 7, 5.5f, true);
		escribir(lienzo, area.renglones(), normal, area.tamano(), Color.BLACK, tx, siguiente - 2 - area.tamano());
		escribir(lienzo, List.of("Biblioteca Benito Juárez"), normal, 5, GRIS, tx, y + 0.22f * CM);
	}

	/** Renglones ya partidos y el tamaño de letra con el que caben. */
	record Bloque(List<String> renglones, float tamano) {

		float alto() {
			return renglones.size() * tamano * INTERLINEADO;
		}

	}

	/**
	 * Acomoda un texto en una caja: prueba de {@code maximo} a {@code minimo} puntos, primero en un solo renglón y
	 * luego partiéndolo. Si ni así cabe, {@code recortar} lo termina en «…»; si no, se queda completo al tamaño
	 * mínimo (el número de inventario nunca se recorta).
	 */
	static Bloque acomodar(String texto, BaseFont fuente, float ancho, float alto, float maximo, float minimo,
			boolean recortar) {
		for (float t = maximo; t >= minimo; t -= 0.5f) {
			if (fuente.getWidthPoint(texto, t) <= ancho && t * INTERLINEADO <= alto) {
				return new Bloque(List.of(texto), t);
			}
		}
		for (float t = maximo; t >= minimo; t -= 0.5f) {
			Bloque bloque = new Bloque(partir(texto, fuente, t, ancho), t);
			if (bloque.alto() <= alto) {
				return bloque;
			}
		}
		List<String> renglones = partir(texto, fuente, minimo, ancho);
		if (!recortar) {
			return new Bloque(renglones, minimo);
		}
		int caben = Math.max(1, (int) Math.floor(alto / (minimo * INTERLINEADO)));
		if (renglones.size() <= caben) {
			return new Bloque(renglones, minimo);
		}
		List<String> visibles = new ArrayList<>(renglones.subList(0, caben));
		String ultimo = visibles.get(caben - 1).stripTrailing();
		while (!ultimo.isEmpty() && fuente.getWidthPoint(ultimo + "…", minimo) > ancho) {
			ultimo = ultimo.substring(0, ultimo.length() - 1).stripTrailing();
		}
		visibles.set(caben - 1, ultimo + "…");
		return new Bloque(visibles, minimo);
	}

	/** Parte en palabras; una palabra más ancha que la caja se parte después de un guion o, si no hay, donde quepa. */
	static List<String> partir(String texto, BaseFont fuente, float tamano, float ancho) {
		List<String> renglones = new ArrayList<>();
		StringBuilder actual = new StringBuilder();
		for (String palabra : texto.trim().split("\\s+")) {
			String candidato = actual.isEmpty() ? palabra : actual + " " + palabra;
			if (fuente.getWidthPoint(candidato, tamano) <= ancho) {
				actual = new StringBuilder(candidato);
				continue;
			}
			if (!actual.isEmpty()) {
				renglones.add(actual.toString());
				actual = new StringBuilder();
			}
			String resto = palabra;
			while (fuente.getWidthPoint(resto, tamano) > ancho) {
				int corte = resto.length() - 1;
				while (corte > 1 && fuente.getWidthPoint(resto.substring(0, corte), tamano) > ancho) {
					corte--;
				}
				int guion = resto.lastIndexOf('-', corte - 1);
				if (guion > 0) {
					corte = guion + 1;
				}
				renglones.add(resto.substring(0, corte));
				resto = resto.substring(corte);
			}
			actual = new StringBuilder(resto);
		}
		if (!actual.isEmpty()) {
			renglones.add(actual.toString());
		}
		return renglones;
	}

	/** Escribe los renglones hacia abajo desde la línea base dada; devuelve dónde termina el último. */
	private static float escribir(PdfContentByte lienzo, List<String> renglones, BaseFont fuente, float tamano,
			Color color, float x, float base) {
		lienzo.beginText();
		lienzo.setFontAndSize(fuente, tamano);
		lienzo.setColorFill(color);
		float linea = base;
		for (String renglon : renglones) {
			lienzo.showTextAligned(Element.ALIGN_LEFT, renglon, x, linea, 0);
			linea -= tamano * INTERLINEADO;
		}
		lienzo.endText();
		return linea + tamano * INTERLINEADO - tamano * 0.25f;
	}

	private static BaseFont fuente(String nombre) {
		try {
			return BaseFont.createFont(nombre, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
		}
		catch (Exception error) {
			throw new IllegalStateException("No se pudo cargar la fuente " + nombre, error);
		}
	}

}
