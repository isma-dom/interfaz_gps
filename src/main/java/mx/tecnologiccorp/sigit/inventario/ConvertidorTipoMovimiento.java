package mx.tecnologiccorp.sigit.inventario;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Guarda el tipo con los valores del diccionario de datos (alta, baja, cambio_estado, ...). */
@Converter(autoApply = true)
public class ConvertidorTipoMovimiento implements AttributeConverter<TipoMovimiento, String> {

	@Override
	public String convertToDatabaseColumn(TipoMovimiento tipo) {
		return tipo == null ? null : tipo.getCodigo();
	}

	@Override
	public TipoMovimiento convertToEntityAttribute(String codigo) {
		return codigo == null ? null : TipoMovimiento.deCodigo(codigo);
	}

}
