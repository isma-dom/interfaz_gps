package mx.tecnologiccorp.sigit.prototipos;

import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import mx.tecnologiccorp.sigit.inventario.EstadoEquipo;
import mx.tecnologiccorp.sigit.seguridad.Pantalla;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.UsuarioRepository;
import mx.tecnologiccorp.sigit.seguridad.UsuarioSesion;

/**
 * Prototipo de P-03 (lista de equipos) y P-05 (alta y edición). Registrar y editar es solo del
 * Administrador (regla en SeguridadConfig); Consulta ve únicamente los equipos de su área.
 */
@Controller
@Profile("dev")
@RequestMapping("/prototipos/equipos")
public class PrototipoEquiposController {

	private final MuestraDeInventario inventario;

	private final UsuarioRepository usuarios;

	public PrototipoEquiposController(MuestraDeInventario inventario, UsuarioRepository usuarios) {
		this.inventario = inventario;
		this.usuarios = usuarios;
	}

	@ModelAttribute
	void comunes(Model modelo) {
		modelo.addAttribute("pantallaActiva", Pantalla.EQUIPOS);
		modelo.addAttribute("tipos", inventario.tipos());
		modelo.addAttribute("marcas", inventario.marcas());
		modelo.addAttribute("areas", inventario.areas());
		modelo.addAttribute("estados", EstadoEquipo.values());
	}

	@GetMapping
	public String lista(@RequestParam(name = "q", required = false) String q,
			@RequestParam(name = "area", required = false) Long areaId,
			@RequestParam(name = "tipo", required = false) Long tipoId,
			@RequestParam(name = "estado", required = false) EstadoEquipo estado,
			@RequestParam(name = "pagina", defaultValue = "1") int pagina,
			@AuthenticationPrincipal UsuarioSesion usuario, Model modelo) {
		// Consulta solo ve su área (C-04): el filtro queda fijo.
		MuestraDeInventario.Ubicacion areaFija = null;
		if (usuario.tieneRol(Rol.CONSULTA)) {
			areaFija = usuarios.findById(usuario.id())
				.map(u -> u.getArea() == null ? null : u.getArea().getNombre())
				.flatMap(inventario::areaPorNombre)
				.orElse(null);
			areaId = areaFija == null ? -1L : areaFija.id();
		}

		modelo.addAttribute("equipos", inventario.buscar(q, areaId, tipoId, estado, Math.max(pagina, 1) - 1));
		modelo.addAttribute("totalInventario", inventario.total());
		modelo.addAttribute("q", q == null ? "" : q.trim());
		modelo.addAttribute("areaId", areaId);
		modelo.addAttribute("tipoId", tipoId);
		modelo.addAttribute("estado", estado);
		modelo.addAttribute("areaFija", areaFija);
		modelo.addAttribute("puedeEditar", usuario.tieneRol(Rol.ADMINISTRADOR));
		return "equipos/lista";
	}

	@GetMapping("/nuevo")
	public String nuevo(Model modelo) {
		// Tras «Registrar y agregar otro» llega el formulario anterior como atributo flash.
		EquipoForm formulario = modelo.containsAttribute("equipo") ? (EquipoForm) modelo.getAttribute("equipo")
				: new EquipoForm();
		return vista(modelo, formulario, "equipos/nuevo");
	}

	@PostMapping
	public String registrar(@Valid @ModelAttribute("equipo") EquipoForm formulario, BindingResult resultado,
			@RequestParam(name = "otro", required = false) String otro, Model modelo,
			RedirectAttributes redireccion) {
		validarUnicos(formulario, null, resultado);
		if (resultado.hasErrors()) {
			return vista(modelo, formulario, "equipos/nuevo");
		}
		MuestraDeInventario.Equipo equipo = inventario.guardar(null, formulario);
		redireccion.addFlashAttribute("exito", "Equipo " + equipo.numInventario() + " registrado.");
		if (otro != null) {
			redireccion.addFlashAttribute("equipo", formulario.copiaParaElSiguiente());
			return "redirect:/prototipos/equipos/nuevo";
		}
		return "redirect:/prototipos/equipos";
	}

	@GetMapping("/{id}/editar")
	public String editar(@PathVariable Long id, Model modelo) {
		MuestraDeInventario.Equipo equipo = inventario.equipo(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		modelo.addAttribute("existente", equipo);
		return vista(modelo, aFormulario(equipo), "equipos/editar");
	}

	@PostMapping("/{id}")
	public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("equipo") EquipoForm formulario,
			BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
		MuestraDeInventario.Equipo existente = inventario.equipo(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		validarUnicos(formulario, id, resultado);
		if (resultado.hasErrors()) {
			modelo.addAttribute("existente", existente);
			return vista(modelo, formulario, "equipos/editar");
		}
		MuestraDeInventario.Equipo equipo = inventario.guardar(id, formulario);
		redireccion.addFlashAttribute("exito", "Cambios del equipo " + equipo.numInventario() + " guardados.");
		return "redirect:/prototipos/equipos";
	}

	/** El grupo de características técnicas visible depende del tipo elegido. */
	private String vista(Model modelo, EquipoForm formulario, String nombre) {
		modelo.addAttribute("equipo", formulario);
		modelo.addAttribute("grupoActual", inventario.tipos()
			.stream()
			.filter(t -> t.id().equals(formulario.getTipoId()))
			.map(t -> t.grupo().name())
			.findFirst()
			.orElse(null));
		return nombre;
	}

	private void validarUnicos(EquipoForm formulario, Long id, BindingResult resultado) {
		if (formulario.getNumInventario() != null && inventario.inventarioOcupado(formulario.getNumInventario(), id)) {
			resultado.rejectValue("numInventario", "duplicado", "Ya existe un equipo con este número de inventario.");
		}
		if (formulario.getNumSerie() != null && inventario.serieOcupada(formulario.getNumSerie(), id)) {
			resultado.rejectValue("numSerie", "duplicado", "Ya existe un equipo con este número de serie.");
		}
	}

	/** El prototipo no guarda las características por campo; la edición muestra los datos generales. */
	private static EquipoForm aFormulario(MuestraDeInventario.Equipo equipo) {
		EquipoForm formulario = new EquipoForm();
		formulario.setNumInventario(equipo.numInventario());
		formulario.setNumSerie(equipo.numSerie());
		formulario.setTipoId(equipo.tipo().id());
		formulario.setMarcaId(equipo.marca().id());
		formulario.setModelo(equipo.modelo());
		formulario.setAreaId(equipo.area().id());
		formulario.setSistemaOperativo(equipo.sistemaOperativo());
		formulario.setFechaAdquisicion(equipo.fechaAdquisicion());
		return formulario;
	}

}
