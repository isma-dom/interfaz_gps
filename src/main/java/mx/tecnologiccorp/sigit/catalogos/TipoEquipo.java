package mx.tecnologiccorp.sigit.catalogos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import mx.tecnologiccorp.sigit.inventario.GrupoTecnico;

/** Clasificación del hardware (catálogo tipos_equipo). */
@Entity
@Table(name = "tipos_equipo")
public class TipoEquipo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 60)
	private String nombre;

	@Column(name = "vida_util_anios")
	private Integer vidaUtilAnios;

	/** Qué características técnicas se capturan para este tipo. */
	@Enumerated(EnumType.STRING)
	@Column(name = "grupo_tecnico", nullable = false, length = 20)
	private GrupoTecnico grupo;

	protected TipoEquipo() {
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public Integer getVidaUtilAnios() {
		return vidaUtilAnios;
	}

	public GrupoTecnico getGrupo() {
		return grupo;
	}

}
