package mx.tecnologiccorp.sigit.inventario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

	/** Historial de un equipo, del más reciente al más antiguo. */
	List<Movimiento> findByEquipoIdOrderByFechaDescIdDesc(Long equipoId);

	Optional<Movimiento> findFirstByEquipoIdAndTipoOrderByFechaDescIdDesc(Long equipoId, TipoMovimiento tipo);

}
