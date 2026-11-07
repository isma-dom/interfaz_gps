package mx.tecnologiccorp.sigit.seguridad;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.FlashMap;

/**
 * Revisa al usuario en cada petición: si lo desactivaron pierde el acceso en ese momento, y
 * si le cambiaron el rol o el nombre, la sesión se actualiza sin tener que volver a entrar.
 *
 * <p>No es un @Component: Spring Boot lo registraría también como filtro de servlet.
 */
public class FiltroUsuarioActivo extends OncePerRequestFilter {

	public static final String ACCESO_DESACTIVADO = "Tu acceso fue desactivado. Pide ayuda a Soporte Técnico.";

	private final UsuarioRepository usuarios;

	private final SecurityContextRepository repositorioDeContexto = new HttpSessionSecurityContextRepository();

	public FiltroUsuarioActivo(UsuarioRepository usuarios) {
		this.usuarios = usuarios;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest peticion) {
		String ruta = Peticiones.ruta(peticion);
		return ruta.startsWith("/css/") || ruta.startsWith("/js/") || ruta.startsWith("/fuentes/")
				|| ruta.startsWith("/webjars/") || ruta.equals("/error");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();

		if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioSesion enSesion) {
			Optional<Usuario> usuario = usuarios.findById(enSesion.id());

			if (usuario.isEmpty() || !usuario.get().isActivo()) {
				new SecurityContextLogoutHandler().logout(peticion, respuesta, autenticacion);
				if (Peticiones.esperaJson(peticion)) {
					Peticiones.responderJson(respuesta, HttpServletResponse.SC_UNAUTHORIZED, "Tu acceso fue desactivado.");
					return;
				}
				FlashMap datos = new FlashMap();
				datos.put("aviso", ACCESO_DESACTIVADO);
				Peticiones.redirigirConFlash(peticion, respuesta, "/login", datos);
				return;
			}

			UsuarioSesion actual = UsuarioSesion.de(usuario.get());
			if (!actual.equals(enSesion)) {
				SecurityContext contexto = SecurityContextHolder.createEmptyContext();
				contexto.setAuthentication(
						UsernamePasswordAuthenticationToken.authenticated(actual, null, actual.permisos()));
				SecurityContextHolder.setContext(contexto);
				repositorioDeContexto.saveContext(contexto, peticion, respuesta);
			}
		}

		cadena.doFilter(peticion, respuesta);
	}

}
