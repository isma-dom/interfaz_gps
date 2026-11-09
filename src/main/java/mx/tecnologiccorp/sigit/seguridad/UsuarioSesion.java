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

	/** Hasta dos iniciales del nombre, para el avatar del encabezado. */
	public String iniciales() {
		StringBuilder iniciales = new StringBuilder();
		for (String palabra : nombre.trim().split("\\s+")) {
			if (!palabra.isEmpty() && iniciales.length() < 2) {
				iniciales.appendCodePoint(Character.toUpperCase(palabra.codePointAt(0)));
			}
		}
		return iniciales.toString();
	}

	public boolean tieneRol(String nombreRol) {
		return rol.equals(nombreRol);
	}

	/** Página a la que llega al iniciar sesión: Consulta no tiene acceso al tablero. */
	public String rutaInicio() {
		return tieneRol(Rol.CONSULTA) ? Pantalla.EQUIPOS.getRuta() : Pantalla.TABLERO.getRuta();
	}

}
