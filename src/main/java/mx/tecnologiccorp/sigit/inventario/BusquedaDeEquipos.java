package mx.tecnologiccorp.sigit.inventario;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

/**
 * Consulta del listado. El texto se busca en los datos de identificación del equipo: número de
 * inventario, número de serie, nombre y modelo; un código QR leído se busca completo. La intercalación
 * de MySQL (utf8mb4_unicode_ci) ignora mayúsculas y acentos.
 */
final class BusquedaDeEquipos {

	private static final Pattern UUID = Pattern
		.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	private BusquedaDeEquipos() {
	}

	static Specification<Equipo> de(FiltroEquipos filtro) {
		return (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			if (filtro.texto() != null) {
				if (UUID.matcher(filtro.texto()).matches()) {
					condiciones.add(cb.equal(raiz.get("qrUuid"), filtro.texto().toLowerCase()));
				}
				else {
					String patron = "%" + escapar(filtro.texto()) + "%";
					condiciones.add(cb.or(cb.like(raiz.get("numInventario"), patron, '\\'),
							cb.like(raiz.get("numSerie"), patron, '\\'), cb.like(raiz.get("nombre"), patron, '\\'),
							cb.like(raiz.get("modelo"), patron, '\\')));
				}
			}
			if (filtro.areaId() != null) {
				condiciones.add(cb.equal(raiz.get("area").get("id"), filtro.areaId()));
			}
			if (filtro.tipoId() != null) {
				condiciones.add(cb.equal(raiz.get("tipo").get("id"), filtro.tipoId()));
			}
			if (filtro.estado() != null) {
				condiciones.add(cb.equal(raiz.get("estado"), filtro.estado()));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
	}

	/** Un % o un _ escritos por la persona se buscan como texto, no como comodines. */
	private static String escapar(String texto) {
		return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
