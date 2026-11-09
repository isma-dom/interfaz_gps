package mx.tecnologiccorp.sigit.inventario;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import mx.tecnologiccorp.sigit.catalogos.Area;
import mx.tecnologiccorp.sigit.seguridad.Usuario;

/**
 * Un movimiento del historial de un equipo (tabla movimientos, AD 2.2.2): cada transición de estado
 * deja uno (C-12). Solo se insertan; nunca se editan ni se borran.
 */
@Entity
@Table(name = "movimientos")
public class Movimiento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "equipo_id", nullable = false, updatable = false)
	private Equipo equipo;

	/** Quién lo registró; vacío si lo registró una tarea automática. */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "usuario_id", updatable = false)
	private Usuario usuario;

	@Column(nullable = false, length = 20, updatable = false)
	private TipoMovimiento tipo;

	@Column(name = "estado_anterior", length = 20, updatable = false)
	private EstadoEquipo estadoAnterior;

	@Column(name = "estado_nuevo", length = 20, updatable = false)
	private EstadoEquipo estadoNuevo;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "area_destino_id", updatable = false)
	private Area areaDestino;

	@Column(columnDefinition = "text", updatable = false)
	private String motivo;

	@Column(nullable = false, updatable = false)
	private LocalDateTime fecha;

	protected Movimiento() {
	}

	private Movimiento(Equipo equipo, Usuario usuario, TipoMovimiento tipo, EstadoEquipo estadoAnterior,
			EstadoEquipo estadoNuevo, String motivo, LocalDateTime fecha) {
		this.equipo = equipo;
		this.usuario = usuario;
		this.tipo = tipo;
		this.estadoAnterior = estadoAnterior;
		this.estadoNuevo = estadoNuevo;
		this.motivo = motivo;
		this.fecha = fecha;
	}

	public static Movimiento alta(Equipo equipo, Usuario usuario, LocalDateTime fecha) {
		Movimiento movimiento = new Movimiento(equipo, usuario, TipoMovimiento.ALTA, null, equipo.getEstado(), null,
				fecha);
		movimiento.areaDestino = equipo.getArea();
		return movimiento;
	}

	public static Movimiento cambioDeEstado(Equipo equipo, Usuario usuario, EstadoEquipo anterior, String motivo,
			LocalDateTime fecha) {
		return new Movimiento(equipo, usuario, TipoMovimiento.CAMBIO_ESTADO, anterior, equipo.getEstado(), motivo,
				fecha);
	}

	public static Movimiento baja(Equipo equipo, Usuario usuario, EstadoEquipo anterior, String motivo,
			LocalDateTime fecha) {
		return new Movimiento(equipo, usuario, TipoMovimiento.BAJA, anterior, EstadoEquipo.BAJA, motivo, fecha);
	}

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public TipoMovimiento getTipo() {
		return tipo;
	}

	public EstadoEquipo getEstadoAnterior() {
		return estadoAnterior;
	}

	public EstadoEquipo getEstadoNuevo() {
		return estadoNuevo;
	}

	public Area getAreaDestino() {
		return areaDestino;
	}

	public String getMotivo() {
		return motivo;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

}
