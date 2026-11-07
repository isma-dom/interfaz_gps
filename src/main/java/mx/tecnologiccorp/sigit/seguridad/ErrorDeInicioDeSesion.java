package mx.tecnologiccorp.sigit.seguridad;

import java.util.Map;

import org.springframework.security.core.AuthenticationException;

/** Falla de inicio de sesión con los mensajes por campo que se muestran en P-01. */
public class ErrorDeInicioDeSesion extends AuthenticationException {

	public static final String CREDENCIALES_INCORRECTAS = "Correo o contraseña incorrectos.";

	private final Map<String, String> errores;

	public ErrorDeInicioDeSesion(Map<String, String> errores) {
		super(String.join(" ", errores.values()));
		this.errores = Map.copyOf(errores);
	}

	public static ErrorDeInicioDeSesion credencialesIncorrectas() {
		return new ErrorDeInicioDeSesion(Map.of("correo", CREDENCIALES_INCORRECTAS));
	}

	public static ErrorDeInicioDeSesion cuentaBloqueada(long minutos) {
		String unidad = minutos == 1 ? "minuto" : "minutos";
		return new ErrorDeInicioDeSesion(
				Map.of("correo", "Demasiados intentos fallidos. Intenta de nuevo en " + minutos + " " + unidad + "."));
	}

	public Map<String, String> getErrores() {
		return errores;
	}

}
