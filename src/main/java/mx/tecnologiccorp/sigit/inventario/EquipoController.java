package mx.tecnologiccorp.sigit.inventario;

import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

import mx.tecnologiccorp.sigit.catalogos.Area;
import mx.tecnologiccorp.sigit.seguridad.Pantalla;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.UsuarioRepository;
import mx.tecnologiccorp.sigit.seguridad.UsuarioSesion;

/**
 * C-09. Equipos: listado con búsqueda (P-03), ficha del equipo, alta y edición (P-05). Consultar es de
 * todos los roles, y Consulta solo ve su área; registrar y editar es del Administrador (reglas en
 * SeguridadConfig).
 */
@Controller
@RequestMapping("/equipos")
public class EquipoController {

	/** Área que no existe: un usuario Consulta sin área asignada no ve ningún equipo. */
	private static final long SIN_AREA = -1L;

	private final EquipoService servicio;

	private final UsuarioRepository usuarios;

	public EquipoController(EquipoService servicio, UsuarioRepository usuarios) {
		this.servicio = servicio;
		this.usuarios = usuarios;
	}

	@ModelAttribute
	void comunes(Model modelo) {
		modelo.addAttribute("pantallaActiva", Pantalla.EQUIPOS);
	}

	@GetMapping
	public String listado(@RequestParam(name = "q", required = false) String q,
			@RequestParam(name = "area", required = false) Long areaId,
			@RequestParam(name = "tipo", required = false) Long tipoId,
			@RequestParam(name = "estado", required = false) EstadoEquipo estado,
			@RequestParam(name = "orden", required = false) String orden,
			@RequestParam(name = "pagina", defaultValue = "1") int pagina,
			@AuthenticationPrincipal UsuarioSesion usuario, Model modelo) {
		Area areaFija = null;
		if (usuario.tieneRol(Rol.CONSULTA)) {
			areaFija = areaDe(usuario);
			areaId = areaFija == null ? SIN_AREA : areaFija.getId();
		}
		FiltroEquipos filtro = new FiltroEquipos(q, areaId, tipoId, estado, orden);
		Page<FilaEquipo> equipos = servicio.buscar(filtro, pagina - 1).map(FilaEquipo::de);
		modelo.addAttribute("equipos", equipos);
		// Con miles de equipos no caben todos los números: se muestran hasta cinco alrededor de la actual.
		int actual = equipos.getNumber() + 1;
		modelo.addAttribute("paginaDesde", Math.max(1, Math.min(actual - 2, equipos.getTotalPages() - 4)));
		modelo.addAttribute("paginaHasta", Math.max(1, Math.min(equipos.getTotalPages(), Math.max(actual + 2, 5))));
		modelo.addAttribute("filtro", filtro);
		modelo.addAttribute("areaFija", areaFija);
		modelo.addAttribute("consultaSinArea", usuario.tieneRol(Rol.CONSULTA) && areaFija == null);
		modelo.addAttribute("totalInventario", servicio.total());
		modelo.addAttribute("areas", servicio.areas());
		modelo.addAttribute("tipos", servicio.tipos());
		modelo.addAttribute("estados", EstadoEquipo.values());
		modelo.addAttribute("puedeEditar", usuario.tieneRol(Rol.ADMINISTRADOR));
		return "equipos/listado";
	}

	/** Ficha del equipo: toda su información, también tras guardar una edición. */
	@GetMapping("/{id}")
	public String detalle(@PathVariable Long id, @AuthenticationPrincipal UsuarioSesion usuario, Model modelo) {
		Equipo equipo = visible(id, usuario);
		modelo.addAttribute("e", equipo);
		modelo.addAttribute("caracteristicas", FichaEquipo.caracteristicas(equipo));
		modelo.addAttribute("costo", FichaEquipo.costo(equipo.getCosto()));
		modelo.addAttribute("puedeEditar",
				usuario.tieneRol(Rol.ADMINISTRADOR) && equipo.getEstado() != EstadoEquipo.BAJA);
		return "equipos/detalle";
	}

	@GetMapping("/nuevo")
	public String nuevo(Model modelo) {
		// Tras «Guardar y agregar otro» llega el formulario anterior como atributo flash.
		EquipoForm formulario = modelo.containsAttribute("equipo") ? (EquipoForm) modelo.getAttribute("equipo")
				: new EquipoForm();
		return formulario(modelo, formulario, "equipos/registro");
	}

	@PostMapping
	public String registrar(@Valid @ModelAttribute("equipo") EquipoForm formulario, BindingResult resultado,
			@RequestParam(name = "otro", required = false) String otro, Model modelo,
			RedirectAttributes redireccion) {
		marcarDuplicados(formulario, null, resultado);
		if (!resultado.hasErrors()) {
			try {
				Equipo equipo = servicio.registrar(formulario);
				// La confirmación muestra qué se guardó (ver equipos/resumen.html).
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
		return formulario(modelo, formulario, "equipos/registro");
	}

	@GetMapping("/{id}/editar")
	public String editar(@PathVariable Long id, Model modelo, RedirectAttributes redireccion) {
		Equipo equipo = servicio.consultar(id).orElseThrow(() -> new EquipoNoEncontrado(id));
		if (equipo.getEstado() == EstadoEquipo.BAJA) {
			redireccion.addFlashAttribute("error", EdicionRechazada.dadoDeBaja().getMessage());
			return "redirect:/equipos/" + id;
		}
		modelo.addAttribute("existente", equipo);
		return formulario(modelo, EquipoForm.de(equipo), "equipos/edicion");
	}

	@PostMapping("/{id}")
	public String actualizar(@PathVariable Long id, @Valid @ModelAttribute("equipo") EquipoForm formulario,
			BindingResult resultado, Model modelo, RedirectAttributes redireccion) {
		Equipo existente = servicio.consultar(id).orElseThrow(() -> new EquipoNoEncontrado(id));
		marcarDuplicados(formulario, id, resultado);
		if (!resultado.hasErrors()) {
			try {
				redireccion.addFlashAttribute("actualizado", servicio.actualizar(id, formulario));
				return "redirect:/equipos/" + id;
			}
			catch (ErroresDeEquipo errores) {
				errores.getErrores().forEach((campo, mensaje) -> resultado.rejectValue(campo, "regla", mensaje));
			}
			catch (EdicionRechazada rechazo) {
				if (!rechazo.isSimultanea()) {
					redireccion.addFlashAttribute("error", rechazo.getMessage());
					return "redirect:/equipos/" + id;
				}
				return conDatosActuales(id, rechazo.getMessage(), modelo);
			}
			catch (ObjectOptimisticLockingFailureException carrera) {
				return conDatosActuales(id, EdicionRechazada.simultanea().getMessage(), modelo);
			}
			catch (DataIntegrityViolationException carrera) {
				resultado.reject("duplicado", "Ya existe un equipo con este número de inventario o de serie.");
			}
		}
		modelo.addAttribute("existente", existente);
		return formulario(modelo, formulario, "equipos/edicion");
	}

	/** Alguien más guardó primero: se muestran sus datos para no sobrescribirlos a ciegas. */
	private String conDatosActuales(Long id, String mensaje, Model modelo) {
		Equipo actual = servicio.consultar(id).orElseThrow(() -> new EquipoNoEncontrado(id));
		modelo.addAttribute("existente", actual);
		modelo.addAttribute("aviso", mensaje);
		return formulario(modelo, EquipoForm.de(actual), "equipos/edicion");
	}

	private void marcarDuplicados(EquipoForm formulario, Long excepto, BindingResult resultado) {
		servicio.duplicados(formulario, excepto).forEach((campo, mensaje) -> {
			if (!resultado.hasFieldErrors(campo)) {
				resultado.rejectValue(campo, "duplicado", mensaje);
			}
		});
	}

	/** Consulta solo ve los equipos de su área (C-04); fuera de ella, el equipo «no existe». */
	private Equipo visible(Long id, UsuarioSesion usuario) {
		Equipo equipo = servicio.consultar(id).orElseThrow(() -> new EquipoNoEncontrado(id));
		if (usuario.tieneRol(Rol.CONSULTA)) {
			Area area = areaDe(usuario);
			if (area == null || !area.getId().equals(equipo.getArea().getId())) {
				throw new EquipoNoEncontrado(id);
			}
		}
		return equipo;
	}

	private Area areaDe(UsuarioSesion usuario) {
		return usuarios.findById(usuario.id()).map(u -> u.getArea()).orElse(null);
	}

	private String formulario(Model modelo, EquipoForm formulario, String vista) {
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
		return vista;
	}

}
