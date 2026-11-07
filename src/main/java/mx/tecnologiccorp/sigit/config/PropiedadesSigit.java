package mx.tecnologiccorp.sigit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Valores configurables del módulo de seguridad (application.properties, prefijo sigit). */
@ConfigurationProperties("sigit")
public record PropiedadesSigit(Bloqueo bloqueo, Sesion sesion, String seedPassword) {

	/** Bloqueo temporal tras varios intentos fallidos con la misma cuenta (C-01). */
	public record Bloqueo(int intentos, int minutos) {
	}

	/**
	 * Minutos de inactividad antes de expirar, minutos antes en que se avisa y si la cookie
	 * solo viaja por HTTPS.
	 */
	public record Sesion(int minutos, int avisoMinutos, boolean cookieSegura) {
	}

}
