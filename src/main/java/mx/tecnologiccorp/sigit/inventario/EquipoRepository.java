package mx.tecnologiccorp.sigit.inventario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

	boolean existsByNumInventario(String numInventario);

	boolean existsByNumSerie(String numSerie);

	Optional<Equipo> findByNumInventario(String numInventario);

}
