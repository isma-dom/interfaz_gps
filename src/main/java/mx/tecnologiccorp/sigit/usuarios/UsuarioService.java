package mx.tecnologiccorp.sigit.usuarios;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import mx.tecnologiccorp.sigit.catalogos.Area;
import mx.tecnologiccorp.sigit.catalogos.AreaRepository;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.RolRepository;
import mx.tecnologiccorp.sigit.seguridad.Usuario;
import mx.tecnologiccorp.sigit.seguridad.UsuarioRepository;

/** C-02. Reglas para gestionar usuarios: nadie deja al sistema sin administradores. */
@Service
public class UsuarioService {

	public static final int POR_PAGINA = 15;

	private final UsuarioRepository usuarios;

	private final RolRepository roles;

	private final AreaRepository areas;

	private final PasswordEncoder cifrador;

	public UsuarioService(UsuarioRepository usuarios, RolRepository roles, AreaRepository areas,
			PasswordEncoder cifrador) {
		this.usuarios = usuarios;
		this.roles = roles;
		this.areas = areas;
		this.cifrador = cifrador;
	}

	@Transactional(readOnly = true)
	public Page<Usuario> buscar(String texto, Long rolId, int pagina) {
		String patron = texto == null || texto.isBlank() ? null
				: "%" + texto.trim().toLowerCase().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
		return usuarios.buscar(patron, rolId, PageRequest.of(Math.max(pagina, 0), POR_PAGINA));
	}

	@Transactional(readOnly = true)
	public Usuario obtener(Long id) {
		return usuarios.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	@Transactional
	public Usuario crear(UsuarioForm formulario) {
		Datos datos = validar(formulario, null);
		Usuario usuario = new Usuario(formulario.getNombre(), formulario.getCorreo(),
				cifrador.encode(formulario.getPassword()), datos.rol(), datos.area());
		return usuarios.save(usuario);
	}

	@Transactional
	public Usuario actualizar(Long id, UsuarioForm formulario, Long actorId) {
		Usuario usuario = obtener(id);
		Datos datos = validar(formulario, usuario);
		boolean dejaDeSerAdministrador = usuario.tieneRol(Rol.ADMINISTRADOR) && !datos.rol().es(Rol.ADMINISTRADOR);

		if (dejaDeSerAdministrador) {
			if (usuario.getId().equals(actorId)) {
				throw ErroresDeUsuario.de("rolId", "No puedes quitarte el rol de Administrador.");
			}
			exigirOtroAdministradorActivo(usuario, "rolId");
		}

		usuario.setNombre(formulario.getNombre());
		usuario.setCorreo(formulario.getCorreo());
		usuario.setRol(datos.rol());
		usuario.setArea(datos.area());
		if (formulario.getPassword() != null && !formulario.getPassword().isEmpty()) {
			usuario.setPassword(cifrador.encode(formulario.getPassword()));
		}
		return usuario;
	}

	@Transactional
	public Usuario desactivar(Long id, Long actorId) {
		Usuario usuario = obtener(id);
		if (usuario.getId().equals(actorId)) {
			throw ErroresDeUsuario.de(ErroresDeUsuario.GENERAL, "No puedes desactivar tu propia cuenta.");
		}
		if (usuario.tieneRol(Rol.ADMINISTRADOR) && usuario.isActivo()) {
			exigirOtroAdministradorActivo(usuario, ErroresDeUsuario.GENERAL);
		}
		usuario.setActivo(false);
		return usuario;
	}

	@Transactional
	public Usuario reactivar(Long id) {
		Usuario usuario = obtener(id);
		usuario.setActivo(true);
		return usuario;
	}

	@Transactional(readOnly = true)
	public List<Rol> roles() {
		return roles.findAllByOrderByNombreAsc();
	}

	@Transactional(readOnly = true)
	public List<Area> areas() {
		return areas.findAllByOrderByNombreAsc();
	}

	/**
	 * Bloquea las filas de los administradores activos (SELECT ... FOR UPDATE): si dos
	 * administradores se desactivan entre sí al mismo tiempo, el segundo espera y ve el
	 * resultado del primero.
	 */
	private void exigirOtroAdministradorActivo(Usuario usuario, String campo) {
		boolean hayOtro = usuarios.bloquearAdministradoresActivos()
			.stream()
			.anyMatch(administrador -> !administrador.getId().equals(usuario.getId()));
		if (!hayOtro) {
			throw ErroresDeUsuario.de(campo, "El sistema necesita al menos un administrador activo.");
		}
	}

	/** Reglas que dependen de varios campos o de la base de datos. */
	private Datos validar(UsuarioForm formulario, Usuario existente) {
		Map<String, String> errores = new LinkedHashMap<>();

		String correo = formulario.getCorreo();
		boolean correoOcupado = existente == null ? usuarios.existsByCorreo(correo)
				: usuarios.existsByCorreoAndIdNot(correo, existente.getId());
		if (correoOcupado) {
			errores.put("correo", "Ya existe un usuario con este correo.");
		}

		Rol rol = formulario.getRolId() == null ? null : roles.findById(formulario.getRolId()).orElse(null);
		if (rol == null) {
			errores.put("rolId", "Elige un rol.");
		}

		Area area = null;
		if (rol != null && rol.es(Rol.CONSULTA)) {
			area = formulario.getAreaId() == null ? null : areas.findById(formulario.getAreaId()).orElse(null);
			if (area == null) {
				errores.put("areaId", "Elige el área de este usuario.");
			}
		}

		String password = formulario.getPassword();
		boolean sinPassword = password == null || password.isEmpty();
		if (sinPassword && existente == null) {
			errores.put("password", "Escribe la contraseña.");
		}
		else if (!sinPassword && !cumplePolitica(password)) {
			errores.put("password", "La contraseña debe tener al menos 8 caracteres, con letras y números.");
		}

		if (!errores.isEmpty()) {
			throw new ErroresDeUsuario(errores);
		}
		return new Datos(rol, area);
	}

	/** Supuesto por confirmar: los documentos no definen la política de contraseñas. */
	private static boolean cumplePolitica(String password) {
		return password.length() >= 8 && password.chars().anyMatch(Character::isLetter)
				&& password.chars().anyMatch(Character::isDigit);
	}

	private record Datos(Rol rol, Area area) {
	}

}
