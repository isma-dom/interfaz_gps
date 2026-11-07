package mx.tecnologiccorp.sigit.seguridad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

/** Utilidades para responder desde los filtros de seguridad. */
public final class Peticiones {

	private Peticiones() {
	}

	/** Ruta dentro de la aplicación, sin el contexto de Tomcat (por ejemplo, /sigit). */
	public static String ruta(HttpServletRequest peticion) {
		return peticion.getRequestURI().substring(peticion.getContextPath().length());
	}

	/** La API futura y las llamadas de JavaScript esperan JSON, no páginas. */
	public static boolean esperaJson(HttpServletRequest peticion) {
		String accept = peticion.getHeader("Accept");
		return (accept != null && accept.contains("application/json"))
				|| "XMLHttpRequest".equals(peticion.getHeader("X-Requested-With"));
	}

	/** Mensajes fijos del sistema, sin comillas: no requieren un serializador. */
	public static void responderJson(HttpServletResponse respuesta, int estado, String mensaje) throws IOException {
		respuesta.setStatus(estado);
		respuesta.setContentType("application/json");
		respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
		respuesta.getWriter().write("{\"message\":\"" + mensaje + "\"}");
	}

	/**
	 * Redirige con datos de un solo uso para la siguiente página. Desde un filtro todavía no
	 * existe el FlashMapManager del DispatcherServlet; SessionFlashMapManager es el mismo
	 * que usa Spring MVC por omisión.
	 */
	public static void redirigirConFlash(HttpServletRequest peticion, HttpServletResponse respuesta, String ruta,
			FlashMap datos) throws IOException {
		String destino = peticion.getContextPath() + ruta;
		datos.setTargetRequestPath(destino);
		new SessionFlashMapManager().saveOutputFlashMap(datos, peticion, respuesta);
		respuesta.sendRedirect(destino);
	}

}
