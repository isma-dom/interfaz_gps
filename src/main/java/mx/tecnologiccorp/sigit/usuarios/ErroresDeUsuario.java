package mx.tecnologiccorp.sigit.usuarios;

import java.util.Map;

/** Reglas de negocio no cumplidas, con el campo del formulario al que pertenecen. */
public class ErroresDeUsuario extends RuntimeException {

	/** Errores que no son de un campo, como desactivar la propia cuenta. */
	public static final String GENERAL = "usuario";

	private final Map<String, String> errores;

	public ErroresDeUsuario(Map<String, String> errores) {
		super(String.join(" ", errores.values()));
		this.errores = Map.copyOf(errores);
	}

	public static ErroresDeUsuario de(String campo, String mensaje) {
		return new ErroresDeUsuario(Map.of(campo, mensaje));
	}

	public Map<String, String> getErrores() {
		return errores;
	}

}
