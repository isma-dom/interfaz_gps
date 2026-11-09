package mx.tecnologiccorp.sigit.inventario;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.tecnologiccorp.sigit.catalogos.Area;
import mx.tecnologiccorp.sigit.catalogos.AreaRepository;
import mx.tecnologiccorp.sigit.catalogos.Marca;
import mx.tecnologiccorp.sigit.catalogos.MarcaRepository;
import mx.tecnologiccorp.sigit.catalogos.TipoEquipo;
import mx.tecnologiccorp.sigit.catalogos.TipoEquipoRepository;
import mx.tecnologiccorp.sigit.seguridad.Usuario;
import mx.tecnologiccorp.sigit.seguridad.UsuarioRepository;

/**
 * C-12. Alta, consulta, edición y baja de equipos (RF-02). Los formatos los valida EquipoForm; aquí van las reglas que consultan
 * la base de datos: números únicos (RN-01) y catálogos existentes. La base de datos tiene además sus
 * propias restricciones (únicos, llaves foráneas y CHECK) por si dos altas llegan al mismo tiempo.
 */
@Service
public class EquipoService {

	private final EquipoRepository equipos;

	private final TipoEquipoRepository tipos;

	private final MarcaRepository marcas;

	private final AreaRepository areas;

	private final MovimientoRepository movimientos;

	private final UsuarioRepository usuarios;

	private final Clock reloj;

	public EquipoService(EquipoRepository equipos, TipoEquipoRepository tipos, MarcaRepository marcas,
			AreaRepository areas, MovimientoRepository movimientos, UsuarioRepository usuarios, Clock reloj) {
		this.equipos = equipos;
		this.tipos = tipos;
		this.marcas = marcas;
		this.areas = areas;
		this.movimientos = movimientos;
		this.usuarios = usuarios;
		this.reloj = reloj;
	}

	/**
	 * Números ya registrados (RN-01). El controlador lo consulta aunque haya otros errores, para que la
	 * persona vea todos juntos.
	 */
	@Transactional(readOnly = true)
	public Map<String, String> duplicados(EquipoForm formulario) {
		return duplicados(formulario, null);
	}

	/** Igual que en el alta, pero en la edición el equipo puede conservar sus propios números. */
	@Transactional(readOnly = true)
	public Map<String, String> duplicados(EquipoForm formulario, Long excepto) {
		Map<String, String> errores = new LinkedHashMap<>();
		String inventario = formulario.getNumInventario();
		if (inventario != null && (excepto == null ? equipos.existsByNumInventario(inventario)
				: equipos.existsByNumInventarioAndIdNot(inventario, excepto))) {
			errores.put("numInventario", "Ya existe un equipo con este número de inventario.");
		}
		String serie = formulario.getNumSerie();
		if (serie != null && (excepto == null ? equipos.existsByNumSerie(serie)
				: equipos.existsByNumSerieAndIdNot(serie, excepto))) {
			errores.put("numSerie", "Ya existe un equipo con este número de serie.");
		}
		return errores;
	}

	/** Alta (Tarea 2). Deja el movimiento de alta en el historial, con quién lo registró. */
	@Transactional
	public Equipo registrar(EquipoForm formulario, Long usuarioId) {
		Catalogos c = catalogos(formulario, duplicados(formulario));
		Equipo equipo = Equipo.nuevo(formulario.getNumInventario(), formulario.getNumSerie(), formulario.getNombre(),
				c.tipo(), c.marca(), formulario.getModelo(), c.area());
		copiarDetalles(equipo, c.tipo(), formulario);
		Equipo guardado = equipos.save(equipo);
		movimientos.save(Movimiento.alta(guardado, usuario(usuarioId), ahora()));
		return guardado;
	}

	/**
	 * Baja (Tarea 4, RN-04). Solo un equipo Candidato a baja o en Revisión pendiente pasa a Baja (AD
	 * 2.2.3), con motivo por escrito. El equipo no se borra: cambia de estado, conserva todos sus datos y
	 * la baja queda en su historial con quién la registró y cuándo.
	 */
	@Transactional
	public EquipoDadoDeBaja darDeBaja(Long id, String motivo, Long version, Long usuarioId) {
		Equipo equipo = equipos.findById(id).orElseThrow(() -> new EquipoNoEncontrado(id));
		if (version == null || version != equipo.getVersion()) {
			throw BajaNoPermitida.simultanea();
		}
		if (!equipo.getEstado().admiteBaja()) {
			throw BajaNoPermitida.porEstado(equipo.getEstado());
		}
		String justificacion = motivo == null ? "" : motivo.trim();
		if (justificacion.length() < 10) {
			throw BajaNoPermitida.sinMotivo();
		}
		EstadoEquipo anterior = equipo.getEstado();
		equipo.cambiarEstado(EstadoEquipo.BAJA);
		// Si otra transacción lo modificó entre la lectura y este punto, @Version lo rechaza aquí.
		equipos.saveAndFlush(equipo);
		Movimiento baja = movimientos.save(Movimiento.baja(equipo, usuario(usuarioId), anterior, justificacion, ahora()));
		return new EquipoDadoDeBaja(equipo.getNumInventario(), anterior, justificacion, baja.getFecha());
	}

	/** Historial del equipo, del movimiento más reciente al más antiguo. */
	@Transactional(readOnly = true)
	public List<Movimiento> historial(Long equipoId) {
		return movimientos.findByEquipoIdOrderByFechaDescIdDesc(equipoId);
	}

	/** El movimiento de baja de un equipo dado de baja: quién, cuándo y por qué. */
	@Transactional(readOnly = true)
	public Optional<Movimiento> movimientoDeBaja(Long equipoId) {
		return movimientos.findFirstByEquipoIdAndTipoOrderByFechaDescIdDesc(equipoId, TipoMovimiento.BAJA);
	}

	private Usuario usuario(Long usuarioId) {
		return usuarioId == null ? null : usuarios.findById(usuarioId).orElse(null);
	}

	private LocalDateTime ahora() {
		return LocalDateTime.now(reloj);
	}

	/**
	 * Edición (Tarea 3). Aplica las mismas reglas que el alta; el estado y el código QR no cambian. Rechaza
	 * los equipos dados de baja (RN-04) y los que alguien más guardó después de abrir el formulario.
	 */
	@Transactional
	public EquipoActualizado actualizar(Long id, EquipoForm formulario) {
		Equipo equipo = equipos.findById(id).orElseThrow(() -> new EquipoNoEncontrado(id));
		if (equipo.getEstado() == EstadoEquipo.BAJA) {
			throw EdicionRechazada.dadoDeBaja();
		}
		if (formulario.getVersion() == null || formulario.getVersion() != equipo.getVersion()) {
			throw EdicionRechazada.simultanea();
		}
		Catalogos c = catalogos(formulario, duplicados(formulario, id));
		Map<String, String> antes = FichaEquipo.datosEditables(equipo);
		equipo.actualizarDatosGenerales(formulario.getNumInventario(), formulario.getNumSerie(), formulario.getNombre(),
				c.tipo(), c.marca(), formulario.getModelo(), c.area());
		copiarDetalles(equipo, c.tipo(), formulario);
		List<String> cambios = FichaEquipo.cambios(antes, FichaEquipo.datosEditables(equipo));
		if (!cambios.isEmpty()) {
			// Si otra transacción lo guardó entre la lectura y este punto, @Version lo rechaza aquí.
			equipos.saveAndFlush(equipo);
		}
		return new EquipoActualizado(equipo.getId(), equipo.getNumInventario(), cambios);
	}

	/** Listado (P-03): búsqueda por datos de identificación, filtros y orden, de 15 en 15 en MySQL. */
	@Transactional(readOnly = true)
	public Page<Equipo> buscar(FiltroEquipos filtro, int pagina) {
		Sort orden = switch (filtro.orden() == null ? "" : filtro.orden()) {
			case "nombre" -> Sort.by("nombre", "numInventario");
			case "recientes" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
			default -> Sort.by("numInventario");
		};
		return equipos.findAll(BusquedaDeEquipos.de(filtro),
				PageRequest.of(Math.max(pagina, 0), FiltroEquipos.POR_PAGINA, orden));
	}

	@Transactional(readOnly = true)
	public Optional<Equipo> consultar(Long id) {
		return equipos.findById(id);
	}

	private record Catalogos(TipoEquipo tipo, Marca marca, Area area) {
	}

	/** Los catálogos elegidos deben existir; junta sus errores con los que ya traía la validación. */
	private Catalogos catalogos(EquipoForm formulario, Map<String, String> errores) {
		TipoEquipo tipo = tipos.findById(formulario.getTipoId()).orElse(null);
		if (tipo == null) {
			errores.put("tipoId", "Elige un tipo de equipo de la lista.");
		}
		Marca marca = marcas.findById(formulario.getMarcaId()).orElse(null);
		if (marca == null) {
			errores.put("marcaId", "Elige una marca de la lista.");
		}
		Area area = areas.findById(formulario.getAreaId()).orElse(null);
		if (area == null) {
			errores.put("areaId", "Elige un área de la lista.");
		}
		if (!errores.isEmpty()) {
			throw new ErroresDeEquipo(errores);
		}
		return new Catalogos(tipo, marca, area);
	}

	private static void copiarDetalles(Equipo equipo, TipoEquipo tipo, EquipoForm formulario) {
		equipo.setCaracteristicas(caracteristicas(tipo.getGrupo(), formulario));
		equipo.setSistemaOperativo(tipo.getGrupo() == GrupoTecnico.COMPUTO ? formulario.getSistemaOperativo() : null);
		equipo.setFechaAdquisicion(formulario.getFechaAdquisicion());
		equipo.setCosto(formulario.getCosto());
		equipo.setObservaciones(formulario.getObservaciones());
	}

	@Transactional(readOnly = true)
	public long total() {
		return equipos.count();
	}

	/** Equipos dados de baja, de todas las áreas o de una sola (rol Consulta). */
	@Transactional(readOnly = true)
	public long totalDadosDeBaja(Long areaId) {
		return areaId == null ? equipos.countByEstado(EstadoEquipo.BAJA)
				: equipos.countByEstadoAndAreaId(EstadoEquipo.BAJA, areaId);
	}

	@Transactional(readOnly = true)
	public List<TipoEquipo> tipos() {
		return tipos.findAllByOrderByNombreAsc();
	}

	@Transactional(readOnly = true)
	public List<Marca> marcas() {
		return marcas.findAllByOrderByNombreAsc();
	}

	@Transactional(readOnly = true)
	public List<Area> areas() {
		return areas.findAllByOrderByNombreAsc();
	}

	/** Solo se guardan las características del grupo del tipo elegido; las demás se descartan. */
	static Map<String, Object> caracteristicas(GrupoTecnico grupo, EquipoForm f) {
		Map<String, Object> datos = new LinkedHashMap<>();
		switch (grupo) {
			case COMPUTO -> {
				agregar(datos, "procesador", f.getProcesador());
				agregar(datos, "memoriaGb", f.getMemoriaGb());
				agregar(datos, "almacenamientoGb", f.getAlmacenamientoGb());
				agregar(datos, "tipoAlmacenamiento", f.getTipoAlmacenamiento());
			}
			case IMPRESION -> {
				agregar(datos, "tecnologia", f.getTecnologiaImpresion());
				agregar(datos, "color", f.getImpresionColor());
				agregar(datos, "conexion", f.getConexion());
			}
			case RED -> {
				agregar(datos, "puertos", f.getPuertos());
				agregar(datos, "administrable", f.getAdministrable());
				agregar(datos, "direccionIp", f.getDireccionIp());
			}
			case VIDEO -> {
				agregar(datos, "tamanoPulgadas", f.getTamanoPulgadas());
				agregar(datos, "resolucion", f.getResolucion());
			}
			case ENERGIA -> {
				agregar(datos, "capacidadVa", f.getCapacidadVa());
				agregar(datos, "autonomiaMinutos", f.getAutonomiaMinutos());
			}
			default -> {
			}
		}
		return datos;
	}

	private static void agregar(Map<String, Object> datos, String clave, Object valor) {
		if (valor != null) {
			datos.put(clave, valor);
		}
	}

}
