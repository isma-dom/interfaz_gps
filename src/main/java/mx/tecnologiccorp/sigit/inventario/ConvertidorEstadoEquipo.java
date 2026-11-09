package mx.tecnologiccorp.sigit.inventario;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el estado con los valores del diccionario de datos (operativo, en_reparacion, ...). */
@Converter(autoApply = true)
public class ConvertidorEstadoEquipo implements AttributeConverter<EstadoEquipo, String> {

	@Override
	public String convertToDatabaseColumn(EstadoEquipo estado) {
		return estado == null ? null : estado.getCodigo();
	}

	@Override
	public EstadoEquipo convertToEntityAttribute(String codigo) {
		return codigo == null ? null : EstadoEquipo.deCodigo(codigo);
	}

}
