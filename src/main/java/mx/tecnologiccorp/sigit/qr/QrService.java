package mx.tecnologiccorp.sigit.qr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import mx.tecnologiccorp.sigit.config.PropiedadesSigit;
import mx.tecnologiccorp.sigit.inventario.Equipo;

/**
 * C-26 QrService. El código QR de un equipo contiene solo la URL {@code <servidor>/e/<qr_uuid>}, nunca datos
 * del equipo (RN-05): quien lo escanea tiene que iniciar sesión para ver la ficha. El qr_uuid se asigna al
 * dar de alta el equipo y no cambia, así que la imagen se genera cada vez que se pide y siempre es la
 * misma.
 */
@Service
public class QrService {

	/** Ruta corta que abre la ficha a partir del código (C-26). */
	public static final String RUTA = "/e/";

	/**
	 * Corrección de errores M (15 %): con la URL completa el código queda en 37 × 37 módulos, que en una
	 * etiqueta de 3 cm se leen bien con la cámara de un celular.
	 */
	private static final Map<EncodeHintType, Object> AJUSTES = Map.of(EncodeHintType.ERROR_CORRECTION,
			ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 4, EncodeHintType.CHARACTER_SET, "UTF-8");

	private final String urlPublica;

	public QrService(PropiedadesSigit propiedades) {
		String url = propiedades.urlPublica();
		this.urlPublica = url == null || url.isBlank() ? null : url.trim().replaceAll("/+$", "");
	}

	/** Lo que lleva el código: la URL de escaneo con el identificador del equipo, y nada más. */
	public String contenido(Equipo equipo) {
		String base = urlPublica != null ? urlPublica
				: ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		return base + RUTA + equipo.getQrUuid();
	}

	/** Matriz del código: un módulo por celda, con el margen blanco de 4 módulos incluido. */
	public BitMatrix matriz(String contenido) {
		try {
			return new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 0, 0, AJUSTES);
		}
		catch (WriterException error) {
			throw new IllegalStateException("No se pudo generar el código QR", error);
		}
	}

	/** SVG vectorial: se imprime nítido a cualquier tamaño. */
	public String svg(String contenido, String titulo) {
		BitMatrix m = matriz(contenido);
		StringBuilder trazo = new StringBuilder();
		for (int y = 0; y < m.getHeight(); y++) {
			int x = 0;
			while (x < m.getWidth()) {
				if (!m.get(x, y)) {
					x++;
					continue;
				}
				int inicio = x;
				while (x < m.getWidth() && m.get(x, y)) {
					x++;
				}
				trazo.append('M').append(inicio).append(' ').append(y).append('h').append(x - inicio).append("v1h-")
					.append(x - inicio).append('z');
			}
		}
		return """
				<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %1$d %2$d" shape-rendering="crispEdges" role="img">\
				<title>%3$s</title><rect width="%1$d" height="%2$d" fill="#fff"/><path fill="#000" d="%4$s"/></svg>"""
			.formatted(m.getWidth(), m.getHeight(), escapar(titulo), trazo);
	}

	/** PNG con {@code pixelesPorModulo} píxeles por módulo, para descargar o pegar en otro documento. */
	public byte[] png(String contenido, int pixelesPorModulo) {
		BitMatrix base = matriz(contenido);
		BufferedImage imagen = MatrixToImageWriter.toBufferedImage(escalar(base, pixelesPorModulo));
		try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			ImageIO.write(imagen, "png", salida);
			return salida.toByteArray();
		}
		catch (IOException error) {
			throw new UncheckedIOException(error);
		}
	}

	private static BitMatrix escalar(BitMatrix m, int factor) {
		BitMatrix grande = new BitMatrix(m.getWidth() * factor, m.getHeight() * factor);
		for (int y = 0; y < m.getHeight(); y++) {
			for (int x = 0; x < m.getWidth(); x++) {
				if (m.get(x, y)) {
					grande.setRegion(x * factor, y * factor, factor, factor);
				}
			}
		}
		return grande;
	}

	private static String escapar(String texto) {
		return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

}
