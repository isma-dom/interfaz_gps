package mx.tecnologiccorp.sigit.inventario;

/** Una fila del listado (P-03): la información principal del equipo, ya lista para mostrarse. */
public record FilaEquipo(Long id, String numInventario, String numSerie, String nombre, String resumenTecnico,
		String tipo, String marca, String modelo, String area, String areaDetalle, EstadoEquipo estado) {

	public static FilaEquipo de(Equipo e) {
		return new FilaEquipo(e.getId(), e.getNumInventario(), e.getNumSerie(), e.getNombre(),
				FichaEquipo.resumenTecnico(e), e.getTipo().getNombre(), e.getMarca().getNombre(), e.getModelo(),
				e.getArea().getNombre(), e.getArea().detalle(), e.getEstado());
	}

}
