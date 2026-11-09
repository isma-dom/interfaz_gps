package mx.tecnologiccorp.sigit.inventario;

/**
 * Una edición que no se puede guardar por el estado del equipo, no por los datos capturados: el equipo
 * está dado de baja o alguien más lo guardó mientras se editaba.
 */
public class EdicionRechazada extends RuntimeException {

	private final boolean simultanea;

	private EdicionRechazada(String mensaje, boolean simultanea) {
		super(mensaje);
		this.simultanea = simultanea;
	}

	public static EdicionRechazada dadoDeBaja() {
		return new EdicionRechazada("Un equipo dado de baja conserva sus datos, pero ya no admite cambios.", false);
	}

	public static EdicionRechazada simultanea() {
		return new EdicionRechazada("Alguien más guardó cambios en este equipo mientras lo editabas. "
				+ "Abajo están sus datos actuales: revisa y vuelve a hacer tus cambios.", true);
	}

	public boolean isSimultanea() {
		return simultanea;
	}

}
