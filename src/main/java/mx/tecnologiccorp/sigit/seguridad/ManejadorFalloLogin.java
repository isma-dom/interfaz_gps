package mx.tecnologiccorp.sigit.seguridad;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.FlashMap;

/** Vuelve a P-01 con el mensaje de error y el correo que se escribió. */
@Component
public class ManejadorFalloLogin implements AuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(HttpServletRequest peticion, HttpServletResponse respuesta,
			AuthenticationException error) throws IOException {
		Map<String, String> errores = error instanceof ErrorDeInicioDeSesion propio ? propio.getErrores()
				: Map.of("correo", ErrorDeInicioDeSesion.CREDENCIALES_INCORRECTAS);

		FlashMap datos = new FlashMap();
		datos.put("errores", errores);
		datos.put("correoAnterior", ProveedorDeAutenticacion.normalizarCorreo(peticion.getParameter("correo")));
		Peticiones.redirigirConFlash(peticion, respuesta, "/login", datos);
	}

}
