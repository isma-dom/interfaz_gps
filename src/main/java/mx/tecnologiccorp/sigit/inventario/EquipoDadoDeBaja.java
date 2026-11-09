package mx.tecnologiccorp.sigit.inventario;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Resultado de la baja, para la confirmación en la ficha. Viaja como dato flash. */
public record EquipoDadoDeBaja(String numInventario, EstadoEquipo estadoAnterior, String motivo,
		LocalDateTime fecha) implements Serializable {
}
