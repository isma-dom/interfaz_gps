package mx.tecnologiccorp.sigit.seguridad;

import static mx.tecnologiccorp.sigit.seguridad.Rol.ADMINISTRADOR;
import static mx.tecnologiccorp.sigit.seguridad.Rol.CONSULTA;
import static mx.tecnologiccorp.sigit.seguridad.Rol.EJECUTIVO;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Pantallas del menú y roles que pueden abrirlas (AD 2.2.7). Es la única fuente de los
 * permisos por ruta: SeguridadConfig los aplica y el layout arma el menú con ellos.
 */
public enum Pantalla {

	TABLERO("/tablero", "Tablero", "bar-chart-line", ADMINISTRADOR, EJECUTIVO),
	EQUIPOS("/equipos", "Equipos", "pc-display", ADMINISTRADOR, EJECUTIVO, CONSULTA),
	ESCANEO("/escaneo", "Escaneo de QR", "qr-code-scan", ADMINISTRADOR, EJECUTIVO, CONSULTA),
	RESGUARDOS("/resguardos", "Resguardos", "file-earmark-person", ADMINISTRADOR, EJECUTIVO, CONSULTA),
	IMPORTACIONES("/importaciones", "Importación y cuarentena", "file-earmark-arrow-up", ADMINISTRADOR),
	REPORTES("/reportes", "Reportes", "file-earmark-text", ADMINISTRADOR, EJECUTIVO, CONSULTA),
	CATALOGOS("/catalogos", "Catálogos", "collection", ADMINISTRADOR),
	USUARIOS("/usuarios", "Usuarios", "people", ADMINISTRADOR),
	BITACORA("/bitacora", "Bitácora", "journal-text", ADMINISTRADOR);

	private final String ruta;

	private final String etiqueta;

	private final String icono;

	private final List<String> roles;

	Pantalla(String ruta, String etiqueta, String icono, String... roles) {
		this.ruta = ruta;
		this.etiqueta = etiqueta;
		this.icono = icono;
		this.roles = List.of(roles);
	}

	public static List<Pantalla> paraRol(String rol) {
		return Arrays.stream(values()).filter(p -> p.permite(rol)).toList();
	}

	public static Optional<Pantalla> porRuta(String ruta) {
		return Arrays.stream(values()).filter(p -> p.ruta.equals(ruta)).findFirst();
	}

	public boolean permite(String rol) {
		return roles.contains(rol);
	}

	public String getRuta() {
		return ruta;
	}

	public String getEtiqueta() {
		return etiqueta;
	}

	public String getIcono() {
		return icono;
	}

	public List<String> getRoles() {
		return roles;
	}

}
