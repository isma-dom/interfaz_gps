package mx.tecnologiccorp.sigit.qr;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

import com.google.zxing.common.BitMatrix;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.ColumnText;
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

		// Textos a la derecha del código.
		float tx = qx + LADO_QR + 0.05f * CM, ancho = x + ANCHO - 0.15f * CM - tx;
		ColumnText columna = new ColumnText(lienzo);
		columna.setSimpleColumn(tx, y + 0.45f * CM, tx + ancho, y + ALTO - 0.3f * CM);
		columna.addElement(parrafo("SIGIT", new Font(negrita, 6, Font.NORMAL, GRIS), 0));
		columna.addElement(parrafo(equipo.getNumInventario(), new Font(negrita, 10.5f, Font.NORMAL, Color.BLACK), 2));
		columna.addElement(parrafo(equipo.getArea().getNombre(), new Font(normal, 7, Font.NORMAL, Color.BLACK), 3));
		columna.go();

		ColumnText pie = new ColumnText(lienzo);
		pie.setSimpleColumn(tx, y + 0.15f * CM, tx + ancho, y + 0.5f * CM);
		pie.addElement(parrafo("Biblioteca Benito Juárez", new Font(normal, 5, Font.NORMAL, GRIS), 0));
		pie.go();
	}

	private static Paragraph parrafo(String texto, Font fuente, float espacioAntes) {
		Paragraph parrafo = new Paragraph(new Phrase(texto, fuente));
		parrafo.setLeading(fuente.getSize() * 1.15f);
		parrafo.setSpacingBefore(espacioAntes);
		parrafo.setAlignment(Element.ALIGN_LEFT);
		return parrafo;
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
