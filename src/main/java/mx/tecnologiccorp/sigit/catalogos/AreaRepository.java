package mx.tecnologiccorp.sigit.catalogos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<Area, Long> {

	List<Area> findAllByOrderByNombreAsc();

	Optional<Area> findByNombre(String nombre);

}
