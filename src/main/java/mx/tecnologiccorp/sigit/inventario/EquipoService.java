package mx.tecnologiccorp.sigit.inventario;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.tecnologiccorp.sigit.catalogos.Area;
import mx.tecnologiccorp.sigit.catalogos.AreaRepository;
import mx.tecnologiccorp.sigit.catalogos.Marca;
import mx.tecnologiccorp.sigit.catalogos.MarcaRepository;
import mx.tecnologiccorp.sigit.catalogos.TipoEquipo;
import mx.tecnologiccorp.sigit.catalogos.TipoEquipoRepository;

/**
 * C-12. Alta de equipos (RF-02). Los formatos los valida EquipoForm; aquí van las reglas que consultan
 * la base de datos: números únicos (RN-01) y catálogos existentes. La base de datos tiene además sus
 * propias restricciones (únicos, llaves foráneas y CHECK) por si dos altas llegan al mismo tiempo.
 */
@Service
public class EquipoService {

	private final EquipoRepository equipos;

	private final TipoEquipoRepository tipos;

	private final MarcaRepository marcas;

	private final AreaRepository areas;

	public EquipoService(EquipoRepository equipos, TipoEquipoRepository tipos, MarcaRepository marcas,
			AreaRepository areas) {
		this.equipos = equipos;
		this.tipos = tipos;
		this.marcas = marcas;
		this.areas = areas;
	}

	/**
	 * Números ya registrados (RN-01). El controlador lo consulta aunque haya otros errores, para que la
	 * persona vea todos juntos.
	 */
	@Transactional(readOnly = true)
	public Map<String, String> duplicados(EquipoForm formulario) {
		Map<String, String> errores = new LinkedHashMap<>();
		if (formulario.getNumInventario() != null && equipos.existsByNumInventario(formulario.getNumInventario())) {
			errores.put("numInventario", "Ya existe un equipo con este número de inventario.");
		}
		if (formulario.getNumSerie() != null && equipos.existsByNumSerie(formulario.getNumSerie())) {
			errores.put("numSerie", "Ya existe un equipo con este número de serie.");
		}
		return errores;
	}

	@Transactional
	public Equipo registrar(EquipoForm formulario) {
		Map<String, String> errores = duplicados(formulario);
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

		Equipo equipo = Equipo.nuevo(formulario.getNumInventario(), formulario.getNumSerie(), formulario.getNombre(),
				tipo, marca, formulario.getModelo(), area);
		equipo.setCaracteristicas(caracteristicas(tipo.getGrupo(), formulario));
		equipo.setSistemaOperativo(tipo.getGrupo() == GrupoTecnico.COMPUTO ? formulario.getSistemaOperativo() : null);
		equipo.setFechaAdquisicion(formulario.getFechaAdquisicion());
		equipo.setCosto(formulario.getCosto());
		equipo.setObservaciones(formulario.getObservaciones());
		return equipos.save(equipo);
	}

	@Transactional(readOnly = true)
	public long total() {
		return equipos.count();
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
