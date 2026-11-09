package mx.tecnologiccorp.sigit.inventario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import mx.tecnologiccorp.sigit.catalogos.Area;
import mx.tecnologiccorp.sigit.catalogos.Marca;
import mx.tecnologiccorp.sigit.catalogos.TipoEquipo;

/**
 * C-11. Equipo de hardware del inventario (tabla equipos, AD 2.2.2). Nunca se borra: la baja es un
 * estado. El responsable no se guarda aquí: sale del resguardo activo.
 */
@Entity
@Table(name = "equipos")
public class Equipo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "num_inventario", nullable = false, length = 30)
	private String numInventario;

	@Column(name = "num_serie", nullable = false, length = 60)
	private String numSerie;

	@Column(nullable = false, length = 80)
	private String nombre;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "tipo_equipo_id", nullable = false)
	private TipoEquipo tipo;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "marca_id", nullable = false)
	private Marca marca;

	@Column(nullable = false, length = 80)
	private String modelo;

	/** Características técnicas según el tipo (procesador, memoria, puertos, ...). */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "json")
	private Map<String, Object> caracteristicas = new LinkedHashMap<>();

	@Column(name = "sistema_operativo", length = 60)
	private String sistemaOperativo;

	@Column(name = "fecha_adquisicion")
	private LocalDate fechaAdquisicion;

	@Column(precision = 12, scale = 2)
	private BigDecimal costo;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "area_id", nullable = false)
	private Area area;

	@Column(nullable = false, length = 20)
	private EstadoEquipo estado;

	/** Lo que codifica la etiqueta QR (RN-05): nunca datos del equipo. */
	@Column(name = "qr_uuid", nullable = false, columnDefinition = "char(36)")
	private String qrUuid;

	@Column(columnDefinition = "text")
	private String observaciones;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Equipo() {
	}

	/** Alta manual: todo equipo nuevo entra como Operativo (AD 2.2.3) con su identificador de QR. */
	public static Equipo nuevo(String numInventario, String numSerie, String nombre, TipoEquipo tipo, Marca marca,
			String modelo, Area area) {
		Equipo equipo = new Equipo();
		equipo.numInventario = numInventario;
		equipo.numSerie = numSerie;
		equipo.nombre = nombre;
		equipo.tipo = tipo;
		equipo.marca = marca;
		equipo.modelo = modelo;
		equipo.area = area;
		equipo.estado = EstadoEquipo.OPERATIVO;
		equipo.qrUuid = UUID.randomUUID().toString();
		return equipo;
	}

	public void setCaracteristicas(Map<String, Object> caracteristicas) {
		this.caracteristicas = new LinkedHashMap<>(caracteristicas);
	}

	public void setSistemaOperativo(String sistemaOperativo) {
		this.sistemaOperativo = sistemaOperativo;
	}

	public void setFechaAdquisicion(LocalDate fechaAdquisicion) {
		this.fechaAdquisicion = fechaAdquisicion;
	}

	public void setCosto(BigDecimal costo) {
		this.costo = costo;
	}

	public void setObservaciones(String observaciones) {
		this.observaciones = observaciones;
	}

	public Long getId() {
		return id;
	}

	public String getNumInventario() {
		return numInventario;
	}

	public String getNumSerie() {
		return numSerie;
	}

	public String getNombre() {
		return nombre;
	}

	public TipoEquipo getTipo() {
		return tipo;
	}

	public Marca getMarca() {
		return marca;
	}

	public String getModelo() {
		return modelo;
	}

	public Map<String, Object> getCaracteristicas() {
		return caracteristicas;
	}

	public String getSistemaOperativo() {
		return sistemaOperativo;
	}

	public LocalDate getFechaAdquisicion() {
		return fechaAdquisicion;
	}

	public BigDecimal getCosto() {
		return costo;
	}

	public Area getArea() {
		return area;
	}

	public EstadoEquipo getEstado() {
		return estado;
	}

	public String getQrUuid() {
		return qrUuid;
	}

	public String getObservaciones() {
		return observaciones;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

}
