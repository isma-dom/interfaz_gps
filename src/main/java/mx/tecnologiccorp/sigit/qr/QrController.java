package mx.tecnologiccorp.sigit.qr;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import mx.tecnologiccorp.sigit.inventario.AccesoAEquipos;
import mx.tecnologiccorp.sigit.inventario.Equipo;
import mx.tecnologiccorp.sigit.inventario.EquipoNoEncontrado;
import mx.tecnologiccorp.sigit.inventario.EquipoRepository;
import mx.tecnologiccorp.sigit.inventario.EstadoEquipo;
import mx.tecnologiccorp.sigit.seguridad.UsuarioSesion;

/**
 * Tarea 5. C-14 EscaneoController: el código QR abre la ficha del equipo (con sesión, RN-05). Además, la
 * imagen del código para verla y descargarla, y C-13 EtiquetaController: las etiquetas en PDF, que solo
 * imprime el Administrador (regla en SeguridadConfig).
 */
@Controller
public class QrController {

	/** Más etiquetas que esto en un solo PDF son, casi seguro, un error al seleccionar. */
	private static final int MAXIMO_ETIQUETAS = 240;

	private final QrService qr;

	private final EtiquetasPdf etiquetas;

	private final AccesoAEquipos acceso;

	private final EquipoRepository equipos;

	private final Clock reloj;

	public QrController(QrService qr, EtiquetasPdf etiquetas, AccesoAEquipos acceso, EquipoRepository equipos,
			Clock reloj) {
		this.qr = qr;
		this.etiquetas = etiquetas;
		this.acceso = acceso;
		this.equipos = equipos;
		this.reloj = reloj;
	}

	/**
	 * Lo que abre el celular al escanear la etiqueta. Sin sesión, Spring Security pide iniciarla y luego
	 * regresa aquí; con sesión, lleva a la ficha del equipo.
	 */
	@GetMapping({ QrService.RUTA + "{uuid}", "/equipos/qr/{uuid}" })
	public String escanear(@PathVariable String uuid, @AuthenticationPrincipal UsuarioSesion usuario) {
		return "redirect:/equipos/" + acceso.visiblePorQr(uuid, usuario).getId();
	}

	@GetMapping("/equipos/{id}/qr.svg")
	public ResponseEntity<String> svg(@PathVariable Long id,
			@RequestParam(name = "descargar", required = false) String descargar,
			@AuthenticationPrincipal UsuarioSesion usuario) {
		Equipo equipo = acceso.visible(id, usuario);
		String svg = qr.svg(qr.contenido(equipo), "Código QR del equipo " + equipo.getNumInventario());
		return archivo(new MediaType("image", "svg+xml", StandardCharsets.UTF_8), "QR-" + equipo.getNumInventario() + ".svg",
				descargar != null).body(svg);
	}

	@GetMapping("/equipos/{id}/qr.png")
	public ResponseEntity<byte[]> png(@PathVariable Long id,
			@RequestParam(name = "descargar", required = false) String descargar,
			@AuthenticationPrincipal UsuarioSesion usuario) {
		Equipo equipo = acceso.visible(id, usuario);
		return archivo(MediaType.IMAGE_PNG, "QR-" + equipo.getNumInventario() + ".png", descargar != null)
			.body(qr.png(qr.contenido(equipo), 12));
	}

	/** La etiqueta de un solo equipo, desde su ficha. */
	@GetMapping("/equipos/{id}/etiqueta.pdf")
	public Object etiqueta(@PathVariable Long id, RedirectAttributes redireccion) {
		Equipo equipo = equipos.findById(id).orElseThrow(() -> new EquipoNoEncontrado(id));
		if (equipo.getEstado() == EstadoEquipo.BAJA) {
			redireccion.addFlashAttribute("error", "Un equipo dado de baja ya no lleva etiqueta.");
			return "redirect:/equipos/" + id;
		}
		return pdf(List.of(equipo), "etiqueta-" + equipo.getNumInventario() + ".pdf");
	}

	/** Las etiquetas de los equipos seleccionados en el listado (P-03, RF-05). */
	@GetMapping("/equipos/etiquetas")
	public Object etiquetas(@RequestParam(name = "id", required = false) List<Long> ids,
			RedirectAttributes redireccion) {
		List<Equipo> elegidos = ids == null ? List.of()
				: equipos.findAllById(ids.stream().distinct().limit(MAXIMO_ETIQUETAS).toList())
					.stream()
					.filter(e -> e.getEstado() != EstadoEquipo.BAJA)
					.sorted(Comparator.comparing(Equipo::getNumInventario))
					.toList();
		if (elegidos.isEmpty()) {
			redireccion.addFlashAttribute("error", "Selecciona al menos un equipo activo para imprimir sus etiquetas.");
			return "redirect:/equipos";
		}
		return pdf(elegidos, "etiquetas-qr-" + LocalDate.now(reloj) + ".pdf");
	}

	/** El PDF se abre en el navegador, listo para imprimir o guardar. */
	private ResponseEntity<byte[]> pdf(List<Equipo> lista, String nombre) {
		return archivo(MediaType.APPLICATION_PDF, nombre, false).body(etiquetas.generar(lista));
	}

	private static ResponseEntity.BodyBuilder archivo(MediaType tipo, String nombre, boolean descargar) {
		ContentDisposition disposicion = (descargar ? ContentDisposition.attachment() : ContentDisposition.inline())
			.filename(nombre)
			.build();
		return ResponseEntity.ok().contentType(tipo).header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString());
	}

}
