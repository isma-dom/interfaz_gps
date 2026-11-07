package mx.tecnologiccorp.sigit.prototipos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import mx.tecnologiccorp.sigit.inventario.EstadoEquipo;
import mx.tecnologiccorp.sigit.inventario.GrupoTecnico;

/** Catálogos e inventario de ejemplo, en memoria, para el prototipo. Se pierden al reiniciar. */
@Component
@Profile("dev")
public class MuestraDeInventario {

	public static final int POR_PAGINA = 15;

	public record Tipo(Long id, String nombre, GrupoTecnico grupo) {
	}

	public record Marca(Long id, String nombre) {
	}

	public record Ubicacion(Long id, String nombre, String edificio, String piso) {

		public String detalle() {
			return edificio + ", piso " + piso;
		}

	}

	public record Equipo(Long id, String numInventario, String numSerie, Tipo tipo, Marca marca, String modelo,
			Ubicacion area, String responsable, EstadoEquipo estado, String sistemaOperativo, String resumenTecnico,
			LocalDate fechaAdquisicion) {

		public String nombre() {
			return marca.nombre() + " " + modelo;
		}

	}

	private final List<Tipo> tipos = List.of(new Tipo(1L, "Computadora de escritorio", GrupoTecnico.COMPUTO),
			new Tipo(2L, "Laptop", GrupoTecnico.COMPUTO), new Tipo(3L, "Impresora", GrupoTecnico.IMPRESION),
			new Tipo(4L, "Multifuncional", GrupoTecnico.IMPRESION), new Tipo(5L, "Switch", GrupoTecnico.RED),
			new Tipo(6L, "Access point", GrupoTecnico.RED), new Tipo(7L, "Monitor", GrupoTecnico.VIDEO),
			new Tipo(8L, "Proyector", GrupoTecnico.VIDEO), new Tipo(9L, "No-break", GrupoTecnico.ENERGIA));

	private final List<Marca> marcas = Stream
		.of("Acer", "APC", "BenQ", "Brother", "Cisco", "Dell", "Epson", "HP", "Lenovo", "TP-Link", "Ubiquiti")
		.map(new java.util.function.Function<String, Marca>() {
			private long id;

			@Override
			public Marca apply(String nombre) {
				return new Marca(++id, nombre);
			}
		})
		.toList();

	private final List<Ubicacion> areas = List.of(new Ubicacion(1L, "Dirección", "Edificio principal", "2"),
			new Ubicacion(2L, "Hemeroteca", "Edificio principal", "1"),
			new Ubicacion(3L, "Procesos técnicos", "Edificio principal", "2"),
			new Ubicacion(4L, "Sala de consulta", "Edificio principal", "1"),
			new Ubicacion(5L, "Sala infantil", "Anexo", "1"),
			new Ubicacion(6L, "Soporte Técnico", "Edificio principal", "1"));

	private final List<Equipo> equipos = new CopyOnWriteArrayList<>();

	private final AtomicLong consecutivo = new AtomicLong();

	public MuestraDeInventario() {
		cargarEjemplos();
	}

	public List<Tipo> tipos() {
		return tipos;
	}

	public List<Marca> marcas() {
		return marcas;
	}

	public List<Ubicacion> areas() {
		return areas;
	}

	public Optional<Ubicacion> areaPorNombre(String nombre) {
		return areas.stream().filter(a -> a.nombre().equals(nombre)).findFirst();
	}

	public Optional<Equipo> equipo(Long id) {
		return equipos.stream().filter(e -> e.id().equals(id)).findFirst();
	}

	public boolean inventarioOcupado(String numInventario, Long excepto) {
		return equipos.stream()
			.anyMatch(e -> e.numInventario().equalsIgnoreCase(numInventario) && !e.id().equals(excepto));
	}

	public boolean serieOcupada(String numSerie, Long excepto) {
		return equipos.stream().anyMatch(e -> e.numSerie().equalsIgnoreCase(numSerie) && !e.id().equals(excepto));
	}

	public long total() {
		return equipos.size();
	}

	public Page<Equipo> buscar(String texto, Long areaId, Long tipoId, EstadoEquipo estado, int pagina) {
		String buscado = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
		List<Equipo> encontrados = equipos.stream()
			.filter(e -> buscado.isEmpty() || Stream
				.of(e.numInventario(), e.numSerie(), e.nombre(), e.tipo().nombre(),
						e.responsable() == null ? "" : e.responsable())
				.anyMatch(v -> v.toLowerCase(Locale.ROOT).contains(buscado)))
			.filter(e -> areaId == null || e.area().id().equals(areaId))
			.filter(e -> tipoId == null || e.tipo().id().equals(tipoId))
			.filter(e -> estado == null || e.estado() == estado)
			.sorted(Comparator.comparing(Equipo::numInventario))
			.toList();
		int desde = Math.min(pagina * POR_PAGINA, encontrados.size());
		int hasta = Math.min(desde + POR_PAGINA, encontrados.size());
		return new PageImpl<>(encontrados.subList(desde, hasta), PageRequest.of(pagina, POR_PAGINA),
				encontrados.size());
	}

	public Equipo guardar(Long id, EquipoForm formulario) {
		Tipo tipo = buscar(tipos, Tipo::id, formulario.getTipoId());
		Marca marca = buscar(marcas, Marca::id, formulario.getMarcaId());
		Ubicacion area = buscar(areas, Ubicacion::id, formulario.getAreaId());
		Optional<Equipo> anterior = id == null ? Optional.empty() : equipo(id);
		Equipo equipo = new Equipo(id == null ? consecutivo.incrementAndGet() : id, formulario.getNumInventario(),
				formulario.getNumSerie(), tipo, marca, formulario.getModelo(), area,
				anterior.map(Equipo::responsable).orElse(null),
				anterior.map(Equipo::estado).orElse(EstadoEquipo.OPERATIVO),
				tipo.grupo() == GrupoTecnico.COMPUTO ? formulario.getSistemaOperativo() : null, resumen(tipo, formulario),
				formulario.getFechaAdquisicion());
		anterior.ifPresent(equipos::remove);
		equipos.add(equipo);
		return equipo;
	}

	/** Lo que el listado muestra bajo el nombre del equipo. */
	private static String resumen(Tipo tipo, EquipoForm f) {
		List<String> partes = new ArrayList<>();
		switch (tipo.grupo()) {
			case COMPUTO -> {
				agregar(partes, f.getProcesador());
				if (f.getMemoriaGb() != null) {
					partes.add(f.getMemoriaGb() + " GB RAM");
				}
				agregar(partes, f.getSistemaOperativo());
			}
			case IMPRESION -> {
				agregar(partes, f.getTecnologiaImpresion());
				if (Boolean.TRUE.equals(f.getImpresionColor())) {
					partes.add("Color");
				}
				agregar(partes, f.getConexion());
			}
			case RED -> {
				if (f.getPuertos() != null) {
					partes.add(f.getPuertos() + " puertos");
				}
				agregar(partes, f.getDireccionIp());
			}
			case VIDEO -> {
				if (f.getTamanoPulgadas() != null) {
					partes.add(f.getTamanoPulgadas() + " pulgadas");
				}
				agregar(partes, f.getResolucion());
			}
			case ENERGIA -> {
				if (f.getCapacidadVa() != null) {
					partes.add(f.getCapacidadVa() + " VA");
				}
			}
			default -> {
			}
		}
		return String.join(", ", partes);
	}

	private static void agregar(List<String> partes, String valor) {
		if (valor != null && !valor.isBlank()) {
			partes.add(valor);
		}
	}

	private static <T> T buscar(List<T> lista, java.util.function.Function<T, Long> id, Long buscado) {
		return lista.stream().filter(e -> id.apply(e).equals(buscado)).findFirst().orElseThrow();
	}

	private void ejemplo(String inventario, String serie, long tipo, String marca, String modelo, long area,
			String responsable, EstadoEquipo estado, String so, String resumen, int anio) {
		equipos.add(new Equipo(consecutivo.incrementAndGet(), inventario, serie, buscar(tipos, Tipo::id, tipo),
				marcas.stream().filter(m -> m.nombre().equals(marca)).findFirst().orElseThrow(), modelo,
				buscar(areas, Ubicacion::id, area), responsable, estado, so, resumen, LocalDate.of(anio, 3, 15)));
	}

	private void cargarEjemplos() {
		EstadoEquipo op = EstadoEquipo.OPERATIVO;
		ejemplo("BBJ-0001", "MXL4021A7K", 1, "HP", "ProDesk 400 G7", 1, "Laura Beatriz Cetina Ruiz", op, "Windows 11", "Core i5-10500, 16 GB RAM, Windows 11", 2021);
		ejemplo("BBJ-0002", "PF3K9W2Z", 2, "Lenovo", "ThinkPad E14", 1, "Laura Beatriz Cetina Ruiz", op, "Windows 11", "Ryzen 5 5500U, 16 GB RAM, Windows 11", 2022);
		ejemplo("BBJ-0003", "CN0X4T7Y", 7, "Dell", "P2422H", 1, "Laura Beatriz Cetina Ruiz", op, null, "24 pulgadas, 1920 × 1080", 2022);
		ejemplo("BBJ-0004", "U63241K8N", 4, "Brother", "MFC-L2750DW", 1, null, op, null, "Láser, Red", 2020);
		ejemplo("BBJ-0010", "8CC9310QVZ", 1, "HP", "ProDesk 600 G1", 2, "Mario Canché Uc", EstadoEquipo.CANDIDATO_BAJA, "Windows 7", "Core i5-4570, 8 GB RAM, Windows 7", 2014);
		ejemplo("BBJ-0011", "8CC9310QW3", 1, "HP", "ProDesk 600 G1", 2, "Rosa Pech Dzul", EstadoEquipo.CANDIDATO_BAJA, "Windows 7", "Core i5-4570, 8 GB RAM, Windows 7", 2014);
		ejemplo("BBJ-0012", "X4CJ041255", 3, "Epson", "EcoTank L3250", 2, null, EstadoEquipo.EN_REPARACION, null, "Inyección de tinta, Color, USB", 2022);
		ejemplo("BBJ-0020", "MJ0B7R3A", 1, "Lenovo", "ThinkCentre M70s", 3, "Jorge Euan Can", op, "Windows 11", "Core i5-12400, 16 GB RAM, Windows 11", 2023);
		ejemplo("BBJ-0021", "MJ0B7R3F", 1, "Lenovo", "ThinkCentre M70s", 3, "Patricia Chi Balam", op, "Windows 11", "Core i5-12400, 16 GB RAM, Windows 11", 2023);
		ejemplo("BBJ-0022", "MJ0B7R4C", 1, "Lenovo", "ThinkCentre M70s", 3, "Raúl May Tun", EstadoEquipo.INACTIVO, "Windows 11", "Core i5-12400, 16 GB RAM, Windows 11", 2023);
		ejemplo("BBJ-0023", "E78812F4N", 3, "Brother", "HL-L5100DN", 3, null, op, null, "Láser, Red", 2019);
		ejemplo("BBJ-0030", "4CE1291XBQ", 1, "HP", "EliteDesk 800 G2", 4, "Ana Koh Poot", op, "Windows 10", "Core i5-6500, 8 GB RAM, Windows 10", 2017);
		ejemplo("BBJ-0031", "4CE1291XC2", 1, "HP", "EliteDesk 800 G2", 4, "Ana Koh Poot", op, "Windows 10", "Core i5-6500, 8 GB RAM, Windows 10", 2017);
		ejemplo("BBJ-0032", "4CE1291XC9", 1, "HP", "EliteDesk 800 G2", 4, "Luis Ek Cauich", EstadoEquipo.CANDIDATO_BAJA, "Windows 10", "Core i5-6500, 8 GB RAM, Windows 10", 2017);
		ejemplo("BBJ-0033", "4CE1291XD4", 1, "HP", "EliteDesk 800 G2", 4, null, EstadoEquipo.REVISION_PENDIENTE, "Windows 10", "Datos importados sin conciliar", 2017);
		ejemplo("BBJ-0034", "XBD6J3200M", 8, "Epson", "PowerLite X49", 4, "Ana Koh Poot", op, null, "Proyector XGA", 2021);
		ejemplo("BBJ-0035", "22356F0019", 6, "Ubiquiti", "UniFi U6 Lite", 4, null, op, null, "Wi-Fi 6, 192.168.10.21", 2023);
		ejemplo("BBJ-0040", "DQ7124AB91", 2, "Acer", "Aspire 3", 5, "Gabriela Tzuc Canul", op, "Windows 11", "Core i3-1115G4, 8 GB RAM, Windows 11", 2022);
		ejemplo("BBJ-0041", "DQ7124AB97", 2, "Acer", "Aspire 3", 5, "Gabriela Tzuc Canul", EstadoEquipo.EN_REPARACION, "Windows 11", "Core i3-1115G4, 8 GB RAM, Windows 11", 2022);
		ejemplo("BBJ-0042", "ETF1N05012", 7, "BenQ", "GW2480", 5, null, op, null, "24 pulgadas, 1920 × 1080", 2020);
		ejemplo("BBJ-0050", "FCW2316L0P8", 5, "Cisco", "Catalyst 2960-X", 6, "Carlos Eduardo Uc Pérez", op, null, "48 puertos, 192.168.10.2", 2016);
		ejemplo("BBJ-0051", "22251G4012", 5, "TP-Link", "TL-SG1024", 6, "Carlos Eduardo Uc Pérez", op, null, "24 puertos", 2021);
		ejemplo("BBJ-0052", "3B2201X47781", 9, "APC", "Back-UPS 1500", 6, "Carlos Eduardo Uc Pérez", op, null, "1500 VA", 2022);
		ejemplo("BBJ-0053", "SLR-0091-23", 1, "Dell", "OptiPlex 3080", 6, "Carlos Eduardo Uc Pérez", op, "Ubuntu 24.04", "Core i3-10100, 8 GB RAM, Ubuntu 24.04", 2021);
		ejemplo("BBJ-0054", "5CG90310ZT", 2, "HP", "ProBook 440 G6", 6, null, EstadoEquipo.BAJA, "Windows 10", "Core i5-8265U, 8 GB RAM, Windows 10", 2019);
	}

}
