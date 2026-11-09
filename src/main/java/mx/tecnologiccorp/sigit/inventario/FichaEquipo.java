package mx.tecnologiccorp.sigit.inventario;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Los datos de un equipo tal como se leen en pantalla: las características técnicas con su nombre y
 * unidad, el resumen técnico del listado y la lista de datos que cambiaron en una edición (Tarea 3).
 */
public final class FichaEquipo {

	private static final Locale MEXICO = Locale.forLanguageTag("es-MX");

	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private FichaEquipo() {
	}

	/** Características técnicas capturadas, con su etiqueta, en el orden del formulario. */
	public static Map<String, String> caracteristicas(Equipo equipo) {
		Map<String, Object> c = equipo.getCaracteristicas() == null ? Map.of() : equipo.getCaracteristicas();
		Map<String, String> datos = new LinkedHashMap<>();
		agregar(datos, "Procesador", c.get("procesador"), "");
		agregar(datos, "Memoria RAM", c.get("memoriaGb"), " GB");
		if (c.get("almacenamientoGb") != null) {
			String tipo = c.get("tipoAlmacenamiento") == null ? "" : " " + c.get("tipoAlmacenamiento");
			datos.put("Almacenamiento", c.get("almacenamientoGb") + " GB" + tipo);
		}
		agregar(datos, "Sistema operativo", equipo.getSistemaOperativo(), "");
		agregar(datos, "Tecnología de impresión", c.get("tecnologia"), "");
		agregar(datos, "Conexión", c.get("conexion"), "");
		agregarSiNo(datos, "Imprime a color", c.get("color"));
		agregar(datos, "Número de puertos", c.get("puertos"), "");
		agregar(datos, "Dirección IP", c.get("direccionIp"), "");
		agregarSiNo(datos, "Es administrable", c.get("administrable"));
		agregar(datos, "Tamaño", c.get("tamanoPulgadas"), " pulgadas");
		agregar(datos, "Resolución", c.get("resolucion"), "");
		agregar(datos, "Capacidad", c.get("capacidadVa"), " VA");
		agregar(datos, "Autonomía", c.get("autonomiaMinutos"), " minutos");
		return datos;
	}

	/** Una línea para el listado: lo que distingue a un equipo de otro del mismo modelo. */
	public static String resumenTecnico(Equipo equipo) {
		Map<String, Object> c = equipo.getCaracteristicas() == null ? Map.of() : equipo.getCaracteristicas();
		List<String> partes = new ArrayList<>();
		switch (equipo.getTipo().getGrupo()) {
			case COMPUTO -> {
				sumar(partes, c.get("procesador"), "");
				sumar(partes, c.get("memoriaGb"), " GB");
				sumar(partes, equipo.getSistemaOperativo(), "");
			}
			case IMPRESION -> {
				sumar(partes, c.get("tecnologia"), "");
				if (Boolean.TRUE.equals(c.get("color"))) {
					partes.add("color");
				}
				sumar(partes, c.get("conexion"), "");
			}
			case RED -> {
				sumar(partes, c.get("puertos"), " puertos");
				sumar(partes, c.get("direccionIp"), "");
			}
			case VIDEO -> {
				sumar(partes, c.get("tamanoPulgadas"), " pulgadas");
				sumar(partes, c.get("resolucion"), "");
			}
			case ENERGIA -> {
				sumar(partes, c.get("capacidadVa"), " VA");
				sumar(partes, c.get("autonomiaMinutos"), " min");
			}
			default -> {
			}
		}
		return partes.isEmpty() ? null : String.join(" · ", partes);
	}

	/** Todo lo que se puede editar, ya legible: sirve para saber qué cambió al guardar. */
	public static Map<String, String> datosEditables(Equipo equipo) {
		Map<String, String> datos = new LinkedHashMap<>();
		datos.put("Nombre", equipo.getNombre());
		datos.put("Tipo", equipo.getTipo().getNombre());
		datos.put("Marca", equipo.getMarca().getNombre());
		datos.put("Modelo", equipo.getModelo());
		datos.put("Número de inventario", equipo.getNumInventario());
		datos.put("Número de serie", equipo.getNumSerie());
		datos.put("Área", equipo.getArea().getNombre());
		datos.putAll(caracteristicas(equipo));
		datos.put("Fecha de adquisición",
				equipo.getFechaAdquisicion() == null ? null : FECHA.format(equipo.getFechaAdquisicion()));
		datos.put("Costo", costo(equipo.getCosto()));
		datos.put("Notas", equipo.getObservaciones());
		return datos;
	}

	/** Etiquetas de los datos que difieren entre dos fichas, en el orden del formulario. */
	public static List<String> cambios(Map<String, String> antes, Map<String, String> despues) {
		Set<String> etiquetas = new LinkedHashSet<>(despues.keySet());
		etiquetas.addAll(antes.keySet());
		return etiquetas.stream().filter(e -> !Objects.equals(antes.get(e), despues.get(e))).toList();
	}

	public static String costo(BigDecimal costo) {
		if (costo == null) {
			return null;
		}
		NumberFormat formato = NumberFormat.getCurrencyInstance(MEXICO);
		return formato.format(costo) + " MXN";
	}

	private static void agregar(Map<String, String> datos, String etiqueta, Object valor, String unidad) {
		if (valor != null) {
			datos.put(etiqueta, valor + unidad);
		}
	}

	private static void agregarSiNo(Map<String, String> datos, String etiqueta, Object valor) {
		if (valor instanceof Boolean si) {
			datos.put(etiqueta, si ? "Sí" : "No");
		}
	}

	private static void sumar(List<String> partes, Object valor, String unidad) {
		if (valor != null) {
			partes.add(valor + unidad);
		}
	}

}
