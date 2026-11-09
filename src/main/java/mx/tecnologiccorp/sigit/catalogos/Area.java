package mx.tecnologiccorp.sigit.catalogos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Área o ubicación de la biblioteca. Su catálogo (P-10) es de otra historia. */
@Entity
@Table(name = "areas")
public class Area {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(length = 60)
	private String edificio;

	@Column(length = 20)
	private String piso;

	protected Area() {
	}

	public Area(String nombre, String edificio, String piso) {
		this.nombre = nombre;
		this.edificio = edificio;
		this.piso = piso;
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getEdificio() {
		return edificio;
	}

	public String getPiso() {
		return piso;
	}

	/** Edificio y piso, para confirmar la ubicación al elegir el área. */
	public String detalle() {
		if (edificio == null && piso == null) {
			return nombre;
		}
		return (edificio == null ? "" : edificio) + (piso == null ? "" : (edificio == null ? "Piso " : ", piso ") + piso);
	}

}
