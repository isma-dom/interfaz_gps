package mx.tecnologiccorp.sigit.catalogos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fabricante del equipo (catálogo marcas). */
@Entity
@Table(name = "marcas")
public class Marca {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 60)
	private String nombre;

	protected Marca() {
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

}
