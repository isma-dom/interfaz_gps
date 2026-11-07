package mx.tecnologiccorp.sigit.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import mx.tecnologiccorp.sigit.seguridad.UsuarioSesion;

/**
 * C-01. Muestra P-01; el inicio y cierre de sesión los procesa Spring Security
 * (ProveedorDeAutenticacion y SeguridadConfig).
 */
@Controller
public class AuthController {

	@GetMapping("/login")
	public String login(@AuthenticationPrincipal UsuarioSesion usuario,
			@RequestParam(name = "expirada", required = false) String expirada, Model modelo) {
		if (usuario != null) {
			return "redirect:" + usuario.rutaInicio();
		}
		if (expirada != null && !modelo.containsAttribute("aviso")) {
			modelo.addAttribute("aviso", "Tu sesión expiró por inactividad.");
		}
		return "login";
	}

	@GetMapping("/")
	public String inicio(@AuthenticationPrincipal UsuarioSesion usuario) {
		return "redirect:" + usuario.rutaInicio();
	}

	/** Cualquier petición renueva la sesión; esta es la del botón «Seguir conectado». */
	@PostMapping("/sesion/mantener")
	public ResponseEntity<Void> mantenerSesion() {
		return ResponseEntity.noContent().build();
	}

}
