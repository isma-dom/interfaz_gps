package mx.tecnologiccorp.sigit.inventario;

/** Una baja que el estado del equipo no permite (AD 2.2.3, RN-04), o que llegó tarde. */
public class BajaNoPermitida extends RuntimeException {

	private BajaNoPermitida(String mensaje) {
		super(mensaje);
	}

	public static BajaNoPermitida porEstado(EstadoEquipo estado) {
		if (estado == EstadoEquipo.BAJA) {
			return new BajaNoPermitida("Este equipo ya está dado de baja.");
		}
		return new BajaNoPermitida("Solo se puede dar de baja un equipo Candidato a baja o en Revisión pendiente. "
				+ "Este equipo está " + estado.getEtiqueta() + ".");
	}

	public static BajaNoPermitida simultanea() {
		return new BajaNoPermitida("Alguien más modificó este equipo mientras confirmabas la baja. "
				+ "Revisa sus datos actuales antes de intentarlo de nuevo.");
	}

	public static BajaNoPermitida sinMotivo() {
		return new BajaNoPermitida("La baja necesita un motivo por escrito de al menos 10 caracteres.");
	}

}
