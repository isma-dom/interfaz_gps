package mx.tecnologiccorp.sigit.inventario;

/**
 * Búsqueda del listado (P-03). {@code texto} busca en los datos de identificación; {@code areaId} llega
 * fijo para el rol Consulta, que solo ve su área.
 */
public record FiltroEquipos(String texto, Long areaId, Long tipoId, EstadoEquipo estado, String orden) {

	public static final int POR_PAGINA = 15;

	public FiltroEquipos {
		texto = texto == null || texto.isBlank() ? null : texto.trim();
		orden = "nombre".equals(orden) || "recientes".equals(orden) ? orden : null;
	}

	public boolean activo() {
		return texto != null || tipoId != null || estado != null || orden != null;
	}

}
