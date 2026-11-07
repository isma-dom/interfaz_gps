package mx.tecnologiccorp.sigit.seguridad;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;

/** C-04. Responde 403 en JSON o con la página error/403, según quién pregunta. */
@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

	public static final String SIN_PERMISO = "No tienes permiso para realizar esta acción.";

	public static final String SESION_EXPIRADA = "Tu sesión expiró por inactividad.";

	@Override
	public void handle(HttpServletRequest peticion, HttpServletResponse respuesta, AccessDeniedException error)
			throws IOException {
		// Token CSRF vencido: la página se quedó abierta y la sesión ya no existe.
		if (error instanceof CsrfException) {
			if (Peticiones.esperaJson(peticion)) {
				Peticiones.responderJson(respuesta, HttpServletResponse.SC_FORBIDDEN, SESION_EXPIRADA);
				return;
			}
			FlashMap datos = new FlashMap();
			datos.put("aviso", SESION_EXPIRADA);
			Peticiones.redirigirConFlash(peticion, respuesta, "/login", datos);
			return;
		}

		if (Peticiones.esperaJson(peticion)) {
			Peticiones.responderJson(respuesta, HttpServletResponse.SC_FORBIDDEN, SIN_PERMISO);
			return;
		}
		respuesta.sendError(HttpServletResponse.SC_FORBIDDEN);
	}

}
