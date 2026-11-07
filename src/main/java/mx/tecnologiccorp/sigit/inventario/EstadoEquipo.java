package mx.tecnologiccorp.sigit.inventario;

/**
 * Estados del ciclo de vida de un equipo (RF-08, AD 2.2.3). Cada estado tiene texto, icono y
 * color: el color nunca es la única señal.
 */
public enum EstadoEquipo {

	OPERATIVO("Operativo", "check-circle-fill", "operativo"),
	EN_REPARACION("En reparación", "tools", "reparacion"),
	INACTIVO("Inactivo", "pause-circle", "inactivo"),
	CANDIDATO_BAJA("Candidato a baja", "exclamation-triangle-fill", "candidato"),
	BAJA("Baja", "x-circle", "baja"),
	REVISION_PENDIENTE("Revisión pendiente", "question-circle-fill", "revision");

	private final String etiqueta;

	private final String icono;

	private final String clase;

	EstadoEquipo(String etiqueta, String icono, String clase) {
		this.etiqueta = etiqueta;
		this.icono = icono;
		this.clase = clase;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

	public String getIcono() {
		return icono;
	}

	public String getClase() {
		return clase;
	}

}
