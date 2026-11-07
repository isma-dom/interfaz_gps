package mx.tecnologiccorp.sigit.seguridad;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByCorreo(String correo);

	boolean existsByCorreo(String correo);

	boolean existsByCorreoAndIdNot(String correo, Long id);

	/**
	 * Búsqueda de la lista de usuarios. El texto llega con los comodines escapados con «!»:
	 * MySQL interpreta la barra invertida dentro de las cadenas SQL.
	 */
	@Query("""
			select u from Usuario u
			where (:texto is null or lower(u.nombre) like :texto escape '!' or lower(u.correo) like :texto escape '!')
			  and (:rolId is null or u.rol.id = :rolId)
			order by u.activo desc, u.nombre asc
			""")
	Page<Usuario> buscar(@Param("texto") String texto, @Param("rolId") Long rolId, Pageable pagina);

	/**
	 * Bloquea las filas de los administradores activos: evita que dos administradores se
	 * desactiven entre sí al mismo tiempo y el sistema quede sin ninguno.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Usuario u where u.activo = true and u.rol.nombre = 'Administrador'")
	List<Usuario> bloquearAdministradoresActivos();

}
