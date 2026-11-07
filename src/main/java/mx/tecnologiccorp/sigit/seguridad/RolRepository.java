package mx.tecnologiccorp.sigit.seguridad;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Long> {

	Optional<Rol> findByNombre(String nombre);

	List<Rol> findAllByOrderByNombreAsc();

}
