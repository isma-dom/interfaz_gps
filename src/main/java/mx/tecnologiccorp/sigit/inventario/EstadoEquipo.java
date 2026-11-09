package mx.tecnologiccorp.sigit.inventario;

/**
 * Estados del ciclo de vida de un equipo (RF-08, AD 2.2.3). Cada estado tiene texto, icono y
 * color: el color nunca es la única señal.
 */
public enum EstadoEquipo {

	OPERATIVO("operativo", "Operativo", "check-circle-fill", "operativo"),
	EN_REPARACION("en_reparacion", "En reparación", "tools", "reparacion"),
	INACTIVO("inactivo", "Inactivo", "pause-circle", "inactivo"),
	CANDIDATO_BAJA("candidato_baja", "Candidato a baja", "exclamation-triangle-fill", "candidato"),
	BAJA("baja", "Baja", "x-circle", "baja"),
	REVISION_PENDIENTE("revision_pendiente", "Revisión pendiente", "question-circle-fill", "revision");

	/** Valor guardado en la columna equipos.estado, como en el diccionario de datos. */
	private final String codigo;

	private final String etiqueta;

	private final String icono;

	private final String clase;

	EstadoEquipo(String codigo, String etiqueta, String icono, String clase) {
		this.codigo = codigo;
		this.etiqueta = etiqueta;
		this.icono = icono;
		this.clase = clase;
	}

	/**
	 * Solo un equipo Candidato a baja o en Revisión pendiente (no localizado) puede pasar a Baja
	 * (AD 2.2.3). La Baja es definitiva: no admite más movimientos (RN-04).
	 */
	public boolean admiteBaja() {
		return this == CANDIDATO_BAJA || this == REVISION_PENDIENTE;
	}

	public static EstadoEquipo deCodigo(String codigo) {
		for (EstadoEquipo estado : values()) {
			if (estado.codigo.equals(codigo)) {
				return estado;
			}
		}
		throw new IllegalArgumentException("Estado de equipo desconocido: " + codigo);
	}

	public String getCodigo() {
		return codigo;
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
