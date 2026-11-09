package mx.tecnologiccorp.sigit.catalogos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoEquipoRepository extends JpaRepository<TipoEquipo, Long> {

	List<TipoEquipo> findAllByOrderByNombreAsc();

}
