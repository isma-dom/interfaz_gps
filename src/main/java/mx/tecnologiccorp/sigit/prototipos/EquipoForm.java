package mx.tecnologiccorp.sigit.prototipos;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/** Campos de P-05 según el diccionario de datos (tabla equipos, AD 2.2.2). */
public class EquipoForm {

	// Identificación
	@NotBlank(message = "Escribe el número de inventario.")
	@Size(max = 30, message = "El número de inventario no debe tener más de 30 caracteres.")
	private String numInventario;

	@NotBlank(message = "Escribe el número de serie.")
	@Size(max = 60, message = "El número de serie no debe tener más de 60 caracteres.")
	private String numSerie;

	@NotNull(message = "Elige el tipo de equipo.")
	private Long tipoId;

	@NotNull(message = "Elige la marca.")
	private Long marcaId;

	@NotBlank(message = "Escribe el modelo.")
	@Size(max = 80, message = "El modelo no debe tener más de 80 caracteres.")
	private String modelo;

	// Ubicación
	@NotNull(message = "Elige el área donde está el equipo.")
	private Long areaId;

	// Características técnicas (columna JSON caracteristicas + sistema_operativo)
	@Size(max = 80, message = "El procesador no debe tener más de 80 caracteres.")
	private String procesador;

	@Min(value = 1, message = "La memoria debe ser de al menos 1 GB.")
	@Max(value = 1024, message = "Revisa la memoria: no debe pasar de 1024 GB.")
	private Integer memoriaGb;

	@Min(value = 1, message = "El almacenamiento debe ser de al menos 1 GB.")
	@Max(value = 100000, message = "Revisa el almacenamiento: no debe pasar de 100 000 GB.")
	private Integer almacenamientoGb;

	private String tipoAlmacenamiento;

	@Size(max = 60, message = "El sistema operativo no debe tener más de 60 caracteres.")
	private String sistemaOperativo;

	private String tecnologiaImpresion;

	private Boolean impresionColor;

	private String conexion;

	@Min(value = 1, message = "El número de puertos debe ser al menos 1.")
	@Max(value = 128, message = "Revisa el número de puertos: no debe pasar de 128.")
	private Integer puertos;

	private Boolean administrable;

	@Size(max = 45, message = "La dirección IP no debe tener más de 45 caracteres.")
	private String direccionIp;

	@Min(value = 1, message = "El tamaño debe ser de al menos 1 pulgada.")
	@Max(value = 200, message = "Revisa el tamaño: no debe pasar de 200 pulgadas.")
	private Integer tamanoPulgadas;

	@Size(max = 20, message = "La resolución no debe tener más de 20 caracteres.")
	private String resolucion;

	@Min(value = 1, message = "La capacidad debe ser de al menos 1 VA.")
	private Integer capacidadVa;

	@Min(value = 1, message = "La autonomía debe ser de al menos 1 minuto.")
	private Integer autonomiaMinutos;

	// Adquisición
	@PastOrPresent(message = "La fecha de adquisición no puede ser futura.")
	private LocalDate fechaAdquisicion;

	@DecimalMin(value = "0", message = "El costo no puede ser negativo.")
	@Digits(integer = 10, fraction = 2, message = "Escribe el costo en pesos, con hasta 2 decimales.")
	private BigDecimal costo;

	// Observaciones
	@Size(max = 1000, message = "Las observaciones no deben tener más de 1000 caracteres.")
	private String observaciones;

	/** Para «Registrar y agregar otro»: conserva lo que suele repetirse en un levantamiento. */
	public EquipoForm copiaParaElSiguiente() {
		EquipoForm siguiente = new EquipoForm();
		siguiente.tipoId = tipoId;
		siguiente.marcaId = marcaId;
		siguiente.modelo = modelo;
		siguiente.areaId = areaId;
		siguiente.procesador = procesador;
		siguiente.memoriaGb = memoriaGb;
		siguiente.almacenamientoGb = almacenamientoGb;
		siguiente.tipoAlmacenamiento = tipoAlmacenamiento;
		siguiente.sistemaOperativo = sistemaOperativo;
		siguiente.tecnologiaImpresion = tecnologiaImpresion;
		siguiente.impresionColor = impresionColor;
		siguiente.conexion = conexion;
		siguiente.puertos = puertos;
		siguiente.administrable = administrable;
		siguiente.tamanoPulgadas = tamanoPulgadas;
		siguiente.resolucion = resolucion;
		siguiente.capacidadVa = capacidadVa;
		siguiente.autonomiaMinutos = autonomiaMinutos;
		siguiente.fechaAdquisicion = fechaAdquisicion;
		siguiente.costo = costo;
		return siguiente;
	}

	private static String limpiar(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

	public String getNumInventario() { return numInventario; }
	public void setNumInventario(String v) { numInventario = limpiar(v) == null ? null : limpiar(v).toUpperCase(); }
	public String getNumSerie() { return numSerie; }
	public void setNumSerie(String v) { numSerie = limpiar(v) == null ? null : limpiar(v).toUpperCase(); }
	public Long getTipoId() { return tipoId; }
	public void setTipoId(Long v) { tipoId = v; }
	public Long getMarcaId() { return marcaId; }
	public void setMarcaId(Long v) { marcaId = v; }
	public String getModelo() { return modelo; }
	public void setModelo(String v) { modelo = limpiar(v); }
	public Long getAreaId() { return areaId; }
	public void setAreaId(Long v) { areaId = v; }
	public String getProcesador() { return procesador; }
	public void setProcesador(String v) { procesador = limpiar(v); }
	public Integer getMemoriaGb() { return memoriaGb; }
	public void setMemoriaGb(Integer v) { memoriaGb = v; }
	public Integer getAlmacenamientoGb() { return almacenamientoGb; }
	public void setAlmacenamientoGb(Integer v) { almacenamientoGb = v; }
	public String getTipoAlmacenamiento() { return tipoAlmacenamiento; }
	public void setTipoAlmacenamiento(String v) { tipoAlmacenamiento = limpiar(v); }
	public String getSistemaOperativo() { return sistemaOperativo; }
	public void setSistemaOperativo(String v) { sistemaOperativo = limpiar(v); }
	public String getTecnologiaImpresion() { return tecnologiaImpresion; }
	public void setTecnologiaImpresion(String v) { tecnologiaImpresion = limpiar(v); }
	public Boolean getImpresionColor() { return impresionColor; }
	public void setImpresionColor(Boolean v) { impresionColor = v; }
	public String getConexion() { return conexion; }
	public void setConexion(String v) { conexion = limpiar(v); }
	public Integer getPuertos() { return puertos; }
	public void setPuertos(Integer v) { puertos = v; }
	public Boolean getAdministrable() { return administrable; }
	public void setAdministrable(Boolean v) { administrable = v; }
	public String getDireccionIp() { return direccionIp; }
	public void setDireccionIp(String v) { direccionIp = limpiar(v); }
	public Integer getTamanoPulgadas() { return tamanoPulgadas; }
	public void setTamanoPulgadas(Integer v) { tamanoPulgadas = v; }
	public String getResolucion() { return resolucion; }
	public void setResolucion(String v) { resolucion = limpiar(v); }
	public Integer getCapacidadVa() { return capacidadVa; }
	public void setCapacidadVa(Integer v) { capacidadVa = v; }
	public Integer getAutonomiaMinutos() { return autonomiaMinutos; }
	public void setAutonomiaMinutos(Integer v) { autonomiaMinutos = v; }
	public LocalDate getFechaAdquisicion() { return fechaAdquisicion; }
	public void setFechaAdquisicion(LocalDate v) { fechaAdquisicion = v; }
	public BigDecimal getCosto() { return costo; }
	public void setCosto(BigDecimal v) { costo = v; }
	public String getObservaciones() { return observaciones; }
	public void setObservaciones(String v) { observaciones = limpiar(v); }

}
