package mx.tecnologiccorp.sigit.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Valores configurables de SIGIT (application.properties, prefijo sigit). */
@ConfigurationProperties("sigit")
public record PropiedadesSigit(Bloqueo bloqueo, Sesion sesion, Obsolescencia obsolescencia, String seedPassword,
		String urlPublica) {

	/** Bloqueo temporal tras varios intentos fallidos con la misma cuenta (C-01). */
	public record Bloqueo(int intentos, int minutos) {
	}

	/**
	 * Minutos de inactividad antes de expirar, minutos antes en que se avisa y si la cookie
	 * solo viaja por HTTPS.
	 */
	public record Sesion(int minutos, int avisoMinutos, boolean cookieSegura) {
	}

	/**
	 * RN-06: un equipo es Candidato a baja si tiene al menos {@code anios} de antigüedad o si su sistema
	 * operativo está en {@code soSinSoporte}. Equivale a los parámetros anios_obsolescencia y
	 * so_sin_soporte del diseño, mientras no exista la pantalla de parámetros.
	 */
	public record Obsolescencia(int anios, List<String> soSinSoporte) {
	}

}
