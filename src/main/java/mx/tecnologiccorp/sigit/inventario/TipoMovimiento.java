package mx.tecnologiccorp.sigit.inventario;

/** Tipos de movimiento del historial de un equipo (tabla movimientos, AD 2.2.2). */
public enum TipoMovimiento {

	ALTA("alta", "Alta"),
	REASIGNACION("reasignacion", "Reasignación"),
	ENVIO_REPARACION("envio_reparacion", "Envío a reparación"),
	RETORNO("retorno", "Retorno de reparación"),
	CAMBIO_ESTADO("cambio_estado", "Cambio de estado"),
	BAJA("baja", "Baja");

	private final String codigo;

	private final String etiqueta;

	TipoMovimiento(String codigo, String etiqueta) {
		this.codigo = codigo;
		this.etiqueta = etiqueta;
	}

	public static TipoMovimiento deCodigo(String codigo) {
		for (TipoMovimiento tipo : values()) {
			if (tipo.codigo.equals(codigo)) {
				return tipo;
			}
		}
		throw new IllegalArgumentException("Tipo de movimiento desconocido: " + codigo);
	}

	public String getCodigo() {
		return codigo;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

}
