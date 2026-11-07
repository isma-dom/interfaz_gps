package mx.tecnologiccorp.sigit.config;

import java.time.Clock;

import jakarta.servlet.SessionCookieConfig;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ConfiguracionGeneral {

	/** Reloj inyectable: las pruebas lo adelantan para verificar el bloqueo. */
	@Bean
	Clock reloj() {
		return Clock.systemDefaultZone();
	}

	/**
	 * Duración de la sesión y cookie. Las propiedades server.servlet.session.* solo aplican al
	 * Tomcat embebido; esto se aplica también cuando sigit.war corre en un Tomcat externo.
	 */
	@Bean
	ServletContextInitializer configuracionDeSesion(PropiedadesSigit propiedades) {
		return contexto -> {
			contexto.setSessionTimeout(propiedades.sesion().minutos());
			SessionCookieConfig cookie = contexto.getSessionCookieConfig();
			cookie.setHttpOnly(true);
			cookie.setSecure(propiedades.sesion().cookieSegura());
			cookie.setAttribute("SameSite", "Lax");
		};
	}

}
