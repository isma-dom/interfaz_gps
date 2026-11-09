package mx.tecnologiccorp.sigit.inventario;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** El equipo no existe o, para el rol Consulta, está fuera de su área: en ambos casos, 404. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EquipoNoEncontrado extends RuntimeException {

	public EquipoNoEncontrado(Long id) {
		super("No existe el equipo " + id);
	}

}
