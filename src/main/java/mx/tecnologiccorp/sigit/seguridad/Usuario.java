package mx.tecnologiccorp.sigit.seguridad;

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

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import mx.tecnologiccorp.sigit.catalogos.Area;

/** Persona con acceso a SIGIT. No se borra: se desactiva (C-02). */
@Entity
@Table(name = "usuarios")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "rol_id", nullable = false)
	private Rol rol;

	/** Solo el rol Consulta tiene área. */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "area_id")
	private Area area;

	@Column(nullable = false, length = 120)
	private String nombre;

	@Column(nullable = false, length = 150)
	private String correo;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private boolean activo = true;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected Usuario() {
	}

	public Usuario(String nombre, String correo, String passwordCifrado, Rol rol, Area area) {
		this.nombre = nombre;
		this.correo = correo;
		this.password = passwordCifrado;
		this.rol = rol;
		this.area = area;
	}

	public boolean tieneRol(String... roles) {
		for (String nombreRol : roles) {
			if (rol.es(nombreRol)) {
				return true;
			}
		}
		return false;
	}

	public Long getId() {
		return id;
	}

	public Rol getRol() {
		return rol;
	}

	public void setRol(Rol rol) {
		this.rol = rol;
	}

	public Area getArea() {
		return area;
	}

	public void setArea(Area area) {
		this.area = area;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String passwordCifrado) {
		this.password = passwordCifrado;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}

}
