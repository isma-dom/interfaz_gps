package mx.tecnologiccorp.sigit.inventario;

import java.util.Map;

/** Reglas del alta que dependen de la base de datos, con el campo al que pertenece cada error. */
public class ErroresDeEquipo extends RuntimeException {

	private final Map<String, String> errores;

	public ErroresDeEquipo(Map<String, String> errores) {
		super(String.join(" ", errores.values()));
		this.errores = Map.copyOf(errores);
	}

	public Map<String, String> getErrores() {
		return errores;
	}

}
