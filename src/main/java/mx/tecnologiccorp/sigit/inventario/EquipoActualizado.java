package mx.tecnologiccorp.sigit.inventario;

import java.io.Serializable;
import java.util.List;

/** Resultado de una edición, para la confirmación: qué datos cambiaron. Viaja como dato flash. */
public record EquipoActualizado(Long id, String numInventario, List<String> cambios) implements Serializable {

	public EquipoActualizado {
		cambios = List.copyOf(cambios);
	}

	public boolean sinCambios() {
		return cambios.isEmpty();
	}

}
