package mx.tecnologiccorp.sigit.seguridad;

import java.io.Serializable;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Datos del usuario que se guardan en la sesión. Nunca incluye la contraseña. */
public record UsuarioSesion(Long id, String nombre, String correo, String rol) implements Serializable {

	public static UsuarioSesion de(Usuario usuario) {
		return new UsuarioSesion(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol().getNombre());
	}

	public List<GrantedAuthority> permisos() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
	}

	public boolean tieneRol(String nombreRol) {
		return rol.equals(nombreRol);
	}

	/** Página a la que llega al iniciar sesión: Consulta no tiene acceso al tablero. */
	public String rutaInicio() {
		return tieneRol(Rol.CONSULTA) ? Pantalla.EQUIPOS.getRuta() : Pantalla.TABLERO.getRuta();
	}

}
