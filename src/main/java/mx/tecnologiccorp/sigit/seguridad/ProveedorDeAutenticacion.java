package mx.tecnologiccorp.sigit.seguridad;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * C-01. Valida correo y contraseña. Correo inexistente, usuario inactivo y contraseña
 * incorrecta producen el mismo mensaje y tardan lo mismo, para no revelar qué cuentas
 * existen.
 */
@Component
public class ProveedorDeAutenticacion implements AuthenticationProvider {

	private final UsuarioRepository usuarios;

	private final PasswordEncoder cifrador;

	private final BloqueoDeAcceso bloqueo;

	/** Se compara contra este hash cuando el correo no existe, para igualar el tiempo. */
	private final String hashFicticio;

	public ProveedorDeAutenticacion(UsuarioRepository usuarios, PasswordEncoder cifrador, BloqueoDeAcceso bloqueo) {
		this.usuarios = usuarios;
		this.cifrador = cifrador;
		this.bloqueo = bloqueo;
		this.hashFicticio = cifrador.encode(UUID.randomUUID().toString());
	}

	public static String normalizarCorreo(String correo) {
		return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
	}

	@Override
	public Authentication authenticate(Authentication solicitud) {
		String correo = normalizarCorreo(solicitud.getName());
		String password = solicitud.getCredentials() == null ? "" : solicitud.getCredentials().toString();
		exigirCampos(correo, password);

		if (bloqueo.estaBloqueado(correo)) {
			throw ErrorDeInicioDeSesion.cuentaBloqueada(bloqueo.minutosRestantes(correo));
		}

		Optional<Usuario> usuario = usuarios.findByCorreo(correo);
		boolean passwordValido = cifrador.matches(password, usuario.map(Usuario::getPassword).orElse(hashFicticio));

		if (usuario.isEmpty() || !usuario.get().isActivo() || !passwordValido) {
			bloqueo.registrarFallo(correo);
			if (bloqueo.estaBloqueado(correo)) {
				throw ErrorDeInicioDeSesion.cuentaBloqueada(bloqueo.minutosRestantes(correo));
			}
			throw ErrorDeInicioDeSesion.credencialesIncorrectas();
		}

		bloqueo.limpiar(correo);
		UsuarioSesion sesion = UsuarioSesion.de(usuario.get());
		return UsernamePasswordAuthenticationToken.authenticated(sesion, null, sesion.permisos());
	}

	@Override
	public boolean supports(Class<?> tipo) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(tipo);
	}

	/** Campos vacíos: error de captura, no cuenta como intento fallido. */
	private void exigirCampos(String correo, String password) {
		Map<String, String> errores = new LinkedHashMap<>();
		if (correo.isEmpty()) {
			errores.put("correo", "Escribe el correo.");
		}
		if (password.isEmpty()) {
			errores.put("password", "Escribe la contraseña.");
		}
		if (!errores.isEmpty()) {
			throw new ErrorDeInicioDeSesion(errores);
		}
	}

}
