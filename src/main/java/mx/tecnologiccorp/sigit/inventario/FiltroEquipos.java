package mx.tecnologiccorp.sigit.inventario;

/**
 * Búsqueda del listado (P-03). {@code texto} busca en los datos de identificación; {@code areaId} llega
 * fijo para el rol Consulta, que solo ve su área. Sin estado, el listado muestra solo los equipos activos:
 * los dados de baja se ven eligiendo el estado Baja o {@code conBajas} (Tarea 4).
 */
public record FiltroEquipos(String texto, Long areaId, Long tipoId, EstadoEquipo estado, boolean conBajas,
		String orden) {

	public static final int POR_PAGINA = 15;

	/** Valor del filtro de estado que incluye los equipos dados de baja junto con los activos. */
	public static final String TODOS = "TODOS";

	public FiltroEquipos {
		texto = texto == null || texto.isBlank() ? null : texto.trim();
		orden = "nombre".equals(orden) || "recientes".equals(orden) ? orden : null;
		conBajas = conBajas && estado == null;
	}

	/** El filtro de estado tal como llega en la dirección: vacío (activos), un estado o TODOS. */
	public static FiltroEquipos de(String texto, Long areaId, Long tipoId, String estado, String orden) {
		if (TODOS.equals(estado)) {
			return new FiltroEquipos(texto, areaId, tipoId, null, true, orden);
		}
		EstadoEquipo elegido = null;
		for (EstadoEquipo e : EstadoEquipo.values()) {
			if (e.name().equals(estado)) {
				elegido = e;
			}
		}
		return new FiltroEquipos(texto, areaId, tipoId, elegido, false, orden);
	}

	/** Valor del filtro de estado para la dirección y para marcar la opción elegida. */
	public String estadoElegido() {
		return conBajas ? TODOS : estado == null ? null : estado.name();
	}

	public boolean activo() {
		return texto != null || tipoId != null || estado != null || conBajas || orden != null;
	}

}
