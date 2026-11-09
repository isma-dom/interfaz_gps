package mx.tecnologiccorp.sigit.inventario;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EquipoRepository extends JpaRepository<Equipo, Long>, JpaSpecificationExecutor<Equipo> {

	boolean existsByNumInventario(String numInventario);

	boolean existsByNumSerie(String numSerie);

	/** Para la edición: el número puede repetirse solo en el mismo equipo. */
	boolean existsByNumInventarioAndIdNot(String numInventario, Long id);

	boolean existsByNumSerieAndIdNot(String numSerie, Long id);

	Optional<Equipo> findByNumInventario(String numInventario);

	List<Equipo> findByEstadoIn(Collection<EstadoEquipo> estados);

	long countByEstado(EstadoEquipo estado);

	long countByEstadoAndAreaId(EstadoEquipo estado, Long areaId);

}
