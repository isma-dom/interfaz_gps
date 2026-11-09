package mx.tecnologiccorp.sigit.inventario;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Resumen del equipo recién guardado, para la confirmación. Viaja en la sesión como dato flash. */
public record EquipoRegistrado(String numInventario, String numSerie, String nombre, String tipo,
		String marcaYModelo, String area, String areaDetalle, EstadoEquipo estado, LocalDateTime registrado)
		implements Serializable {

	public static EquipoRegistrado de(Equipo equipo) {
		return new EquipoRegistrado(equipo.getNumInventario(), equipo.getNumSerie(), equipo.getNombre(),
				equipo.getTipo().getNombre(), equipo.getMarca().getNombre() + " " + equipo.getModelo(),
				equipo.getArea().getNombre(), equipo.getArea().detalle(), equipo.getEstado(), equipo.getCreatedAt());
	}

}
