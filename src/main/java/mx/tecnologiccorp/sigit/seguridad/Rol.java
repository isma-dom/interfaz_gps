package mx.tecnologiccorp.sigit.seguridad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Rol {

	public static final String ADMINISTRADOR = "Administrador";

	public static final String EJECUTIVO = "Ejecutivo";

	public static final String CONSULTA = "Consulta";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String nombre;

	protected Rol() {
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public boolean es(String nombreRol) {
		return nombre.equals(nombreRol);
	}

}
