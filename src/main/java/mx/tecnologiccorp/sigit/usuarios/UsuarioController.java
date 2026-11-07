package mx.tecnologiccorp.sigit.usuarios;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import mx.tecnologiccorp.sigit.seguridad.Usuario;
import mx.tecnologiccorp.sigit.seguridad.UsuarioSesion;

/** P-11 (solo Administrador, regla en SeguridadConfig). Los usuarios no se borran. */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

	private final UsuarioService servicio;

	public UsuarioController(UsuarioService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	public String lista(@RequestParam(name = "q", required = false) String q,
			@RequestParam(name = "rol", required = false) Long rolId,
			@RequestParam(name = "pagina", defaultValue = "1") int pagina, Model modelo) {
		modelo.addAttribute("usuarios", servicio.buscar(q, rolId, pagina - 1));
		modelo.addAttribute("roles", servicio.roles());
		modelo.addAttribute("q", q == null ? "" : q.trim());
		modelo.addAttribute("rolId", rolId);
		return "usuarios/lista";
	}

	@GetMapping("/nuevo")
	public String nuevo(Model modelo) {
		return formulario(modelo, new UsuarioForm(), null);
	}

	@PostMapping
	public String crear(@Valid @ModelAttribute("usuario") UsuarioForm formulario, BindingResult resultado,
			Model modelo, RedirectAttributes redireccion) {
		if (!resultado.hasErrors()) {
			try {
				Usuario usuario = servicio.crear(formulario);
				redireccion.addFlashAttribute("exito", "Usuario " + usuario.getNombre() + " agregado.");
				return "redirect:/usuarios";
			}
			catch (ErroresDeUsuario errores) {
				agregar(errores, resultado);
			}
			catch (DataIntegrityViolationException carrera) {
				resultado.rejectValue("correo", "duplicado", "Ya existe un usuario con este correo.");
			}
		}
		return formulario(modelo, formulario, null);
	}

	@GetMapping("/{id}/editar")
	public String editar(@PathVariable Long id, Model modelo) {
		Usuario usuario = servicio.obtener(id);
		return formulario(modelo, UsuarioForm.de(usuario), usuario);
	}

	@PostMapping("/{id}")
	public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("usuario") UsuarioForm formulario,
			BindingResult resultado, @AuthenticationPrincipal UsuarioSesion actor, Model modelo,
			RedirectAttributes redireccion) {
		Usuario existente = servicio.obtener(id);
		if (!resultado.hasErrors()) {
			try {
				Usuario usuario = servicio.actualizar(id, formulario, actor.id());
				redireccion.addFlashAttribute("exito", "Cambios de " + usuario.getNombre() + " guardados.");
				return "redirect:/usuarios";
			}
			catch (ErroresDeUsuario errores) {
				agregar(errores, resultado);
			}
			catch (DataIntegrityViolationException carrera) {
				resultado.rejectValue("correo", "duplicado", "Ya existe un usuario con este correo.");
			}
		}
		return formulario(modelo, formulario, existente);
	}

	@PostMapping("/{id}/desactivar")
	public String desactivar(@PathVariable Long id, @AuthenticationPrincipal UsuarioSesion actor,
			RedirectAttributes redireccion) {
		try {
			Usuario usuario = servicio.desactivar(id, actor.id());
			redireccion.addFlashAttribute("exito", "Usuario " + usuario.getNombre() + " desactivado.");
		}
		catch (ErroresDeUsuario errores) {
			redireccion.addFlashAttribute("error", String.join(" ", errores.getErrores().values()));
		}
		return "redirect:/usuarios";
	}

	@PostMapping("/{id}/reactivar")
	public String reactivar(@PathVariable Long id, RedirectAttributes redireccion) {
		Usuario usuario = servicio.reactivar(id);
		redireccion.addFlashAttribute("exito", "Usuario " + usuario.getNombre() + " reactivado.");
		return "redirect:/usuarios";
	}

	private String formulario(Model modelo, UsuarioForm formulario, Usuario existente) {
		modelo.addAttribute("usuario", formulario);
		modelo.addAttribute("existente", existente);
		modelo.addAttribute("roles", servicio.roles());
		modelo.addAttribute("areas", servicio.areas());
		return existente == null ? "usuarios/nuevo" : "usuarios/editar";
	}

	private static void agregar(ErroresDeUsuario errores, BindingResult resultado) {
		errores.getErrores().forEach((campo, mensaje) -> {
			if (ErroresDeUsuario.GENERAL.equals(campo)) {
				resultado.reject("regla", mensaje);
			}
			else {
				resultado.rejectValue(campo, "regla", mensaje);
			}
		});
	}

}
