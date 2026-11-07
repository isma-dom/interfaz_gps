package mx.tecnologiccorp.sigit.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import mx.tecnologiccorp.sigit.seguridad.ProveedorDeAutenticacion;
import mx.tecnologiccorp.sigit.seguridad.Usuario;

/**
 * Datos del formulario de P-11. Las reglas que dependen de otros campos (área solo para
 * Consulta, contraseña obligatoria en el alta) las aplica UsuarioService.
 */
public class UsuarioForm {

	@NotBlank(message = "Escribe el nombre.")
	@Size(max = 120, message = "El nombre no debe tener más de 120 caracteres.")
	private String nombre;

	@NotBlank(message = "Escribe el correo.")
	@Email(message = "Escribe un correo válido, por ejemplo nombre@dominio.mx.")
	@Size(max = 150, message = "El correo no debe tener más de 150 caracteres.")
	private String correo;

	@NotNull(message = "Elige un rol.")
	private Long rolId;

	private Long areaId;

	/** BCrypt solo usa los primeros 72 bytes. */
	@Size(max = 72, message = "La contraseña no debe tener más de 72 caracteres.")
	private String password;

	public static UsuarioForm de(Usuario usuario) {
		UsuarioForm formulario = new UsuarioForm();
		formulario.nombre = usuario.getNombre();
		formulario.correo = usuario.getCorreo();
		formulario.rolId = usuario.getRol().getId();
		formulario.areaId = usuario.getArea() == null ? null : usuario.getArea().getId();
		return formulario;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre == null ? null : nombre.trim();
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = ProveedorDeAutenticacion.normalizarCorreo(correo);
	}

	public Long getRolId() {
		return rolId;
	}

	public void setRolId(Long rolId) {
		this.rolId = rolId;
	}

	public Long getAreaId() {
		return areaId;
	}

	public void setAreaId(Long areaId) {
		this.areaId = areaId;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
