package mx.tecnologiccorp.sigit.inventario;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import mx.tecnologiccorp.sigit.seguridad.Pantalla;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.UsuarioSesion;

/**
 * C-09. Alta de equipos (P-05). Registrar es solo del Administrador (regla en SeguridadConfig); el
 * listado P-03 con datos reales es una tarea posterior.
 */
@Controller
@RequestMapping("/equipos")
public class EquipoController {

	private final EquipoService servicio;

	public EquipoController(EquipoService servicio) {
		this.servicio = servicio;
	}

	@ModelAttribute
	void comunes(Model modelo) {
		modelo.addAttribute("pantallaActiva", Pantalla.EQUIPOS);
	}

	@GetMapping
	public String inicio(@AuthenticationPrincipal UsuarioSesion usuario, Model modelo) {
		modelo.addAttribute("total", servicio.total());
		modelo.addAttribute("puedeRegistrar", usuario.tieneRol(Rol.ADMINISTRADOR));
		return "equipos/inicio";
	}

	@GetMapping("/nuevo")
	public String nuevo(Model modelo) {
		// Tras «Registrar y agregar otro» llega el formulario anterior como atributo flash.
		EquipoForm formulario = modelo.containsAttribute("equipo") ? (EquipoForm) modelo.getAttribute("equipo")
				: new EquipoForm();
		return formulario(modelo, formulario);
	}

	@PostMapping
	public String registrar(@Valid @ModelAttribute("equipo") EquipoForm formulario, BindingResult resultado,
			@RequestParam(name = "otro", required = false) String otro, Model modelo,
			RedirectAttributes redireccion) {
		servicio.duplicados(formulario).forEach((campo, mensaje) -> {
			if (!resultado.hasFieldErrors(campo)) {
				resultado.rejectValue(campo, "duplicado", mensaje);
			}
		});
		if (!resultado.hasErrors()) {
			try {
				Equipo equipo = servicio.registrar(formulario);
				// La confirmación muestra qué se guardó (ver equipos/inicio.html, fragmento resumen).
				redireccion.addFlashAttribute("registrado", EquipoRegistrado.de(equipo));
				if (otro != null) {
					redireccion.addFlashAttribute("equipo", formulario.copiaParaElSiguiente());
					return "redirect:/equipos/nuevo";
				}
				return "redirect:/equipos";
			}
			catch (ErroresDeEquipo errores) {
				errores.getErrores().forEach((campo, mensaje) -> resultado.rejectValue(campo, "regla", mensaje));
			}
			catch (DataIntegrityViolationException carrera) {
				// Otro registro con el mismo número entró entre la validación y el guardado.
				resultado.reject("duplicado", "Ya existe un equipo con este número de inventario o de serie.");
			}
		}
		return formulario(modelo, formulario);
	}

	private String formulario(Model modelo, EquipoForm formulario) {
		modelo.addAttribute("equipo", formulario);
		modelo.addAttribute("tipos", servicio.tipos());
		modelo.addAttribute("marcas", servicio.marcas());
		modelo.addAttribute("areas", servicio.areas());
		modelo.addAttribute("grupoActual", servicio.tipos()
			.stream()
			.filter(t -> t.getId().equals(formulario.getTipoId()))
			.map(t -> t.getGrupo().name())
			.findFirst()
			.orElse(null));
		return "equipos/registro";
	}

}
