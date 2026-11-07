package mx.tecnologiccorp.sigit.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Quien llegó a una página protegida sin sesión vuelve a ella al entrar (lo necesitará el
 * escaneo de QR, CP-11). Si no, va a la página de inicio de su rol.
 */
@Component
public class ManejadorExitoLogin extends SavedRequestAwareAuthenticationSuccessHandler {

	@Override
	protected String determineTargetUrl(HttpServletRequest peticion, HttpServletResponse respuesta,
			Authentication autenticacion) {
		return ((UsuarioSesion) autenticacion.getPrincipal()).rutaInicio();
	}

}
