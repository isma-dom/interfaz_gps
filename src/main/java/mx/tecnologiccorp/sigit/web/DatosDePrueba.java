package mx.tecnologiccorp.sigit.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import mx.tecnologiccorp.sigit.catalogos.Area;
import mx.tecnologiccorp.sigit.catalogos.AreaRepository;
import mx.tecnologiccorp.sigit.config.PropiedadesSigit;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.RolRepository;
import mx.tecnologiccorp.sigit.seguridad.Usuario;
import mx.tecnologiccorp.sigit.seguridad.UsuarioRepository;

/**
 * Solo con el perfil dev: áreas de ejemplo y un usuario por rol. El catálogo real lo entrega
 * el cliente y los usuarios de prueba nunca existen en producción.
 */
@Component
@Profile("dev")
public class DatosDePrueba implements ApplicationRunner {

	private static final Logger LOG = LoggerFactory.getLogger(DatosDePrueba.class);

	private final AreaRepository areas;

	private final RolRepository roles;

	private final UsuarioRepository usuarios;

	private final PasswordEncoder cifrador;

	private final PropiedadesSigit propiedades;

	public DatosDePrueba(AreaRepository areas, RolRepository roles, UsuarioRepository usuarios,
			PasswordEncoder cifrador, PropiedadesSigit propiedades) {
		this.areas = areas;
		this.roles = roles;
		this.usuarios = usuarios;
		this.cifrador = cifrador;
		this.propiedades = propiedades;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments argumentos) {
		Area sala = area("Sala de consulta", "1");
		area("Procesos técnicos", "2");
		area("Soporte Técnico", "1");

		String password = propiedades.seedPassword();
		if (password == null || password.isBlank()) {
			LOG.warn("SIGIT_SEED_PASSWORD está vacío: no se crearon usuarios de prueba.");
			return;
		}

		usuario("admin@sigit.test", "Administrador de prueba", Rol.ADMINISTRADOR, null, password);
		usuario("ejecutivo@sigit.test", "Ejecutivo de prueba", Rol.EJECUTIVO, null, password);
		usuario("consulta@sigit.test", "Consulta de prueba", Rol.CONSULTA, sala, password);
	}

	private Area area(String nombre, String piso) {
		return areas.findByNombre(nombre).orElseGet(() -> areas.save(new Area(nombre, "Edificio principal", piso)));
	}

	private void usuario(String correo, String nombre, String rol, Area area, String password) {
		if (!usuarios.existsByCorreo(correo)) {
			Rol encontrado = roles.findByNombre(rol).orElseThrow();
			usuarios.save(new Usuario(nombre, correo, cifrador.encode(password), encontrado, area));
		}
	}

}
