package mx.tecnologiccorp.sigit.inventario;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Confirmación de la baja (RN-04): motivo por escrito y la casilla de que es definitiva. */
public class BajaForm {

	@NotBlank(message = "Escribe el motivo de la baja.")
	@Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres.")
	private String motivo;

	@AssertTrue(message = "Marca la casilla para confirmar que la baja es definitiva.")
	private boolean confirmo;

	/** Versión del equipo que se vio al confirmar (bloqueo optimista, como en la edición). */
	private Long version;

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo == null || motivo.isBlank() ? null : motivo.trim();
	}

	public boolean isConfirmo() {
		return confirmo;
	}

	public void setConfirmo(boolean confirmo) {
		this.confirmo = confirmo;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}

}
