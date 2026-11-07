package mx.tecnologiccorp.sigit.web;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import mx.tecnologiccorp.sigit.config.PropiedadesSigit;
import mx.tecnologiccorp.sigit.seguridad.Pantalla;
import mx.tecnologiccorp.sigit.seguridad.Peticiones;
import mx.tecnologiccorp.sigit.seguridad.UsuarioSesion;

/** Datos que usa el layout en todas las páginas: usuario, menú y tiempos de sesión. */
@ControllerAdvice
public class DatosComunes {

	private final PropiedadesSigit propiedades;

	public DatosComunes(PropiedadesSigit propiedades) {
		this.propiedades = propiedades;
	}

	@ModelAttribute("usuarioActual")
	public UsuarioSesion usuarioActual(@AuthenticationPrincipal UsuarioSesion usuario) {
		return usuario;
	}

	@ModelAttribute("menu")
	public List<Pantalla> menu(@AuthenticationPrincipal UsuarioSesion usuario) {
		return usuario == null ? List.of() : Pantalla.paraRol(usuario.rol());
	}

	@ModelAttribute("rutaActual")
	public String rutaActual(HttpServletRequest peticion) {
		return Peticiones.ruta(peticion);
	}

	@ModelAttribute("sesion")
	public PropiedadesSigit.Sesion sesion() {
		return propiedades.sesion();
	}

}
