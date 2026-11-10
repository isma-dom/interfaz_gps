package mx.tecnologiccorp.sigit.config;

import java.time.ZoneId;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;

/**
 * Tarea 6 (defecto D-02). Las fechas de registro, modificación y movimientos se toman del reloj del
 * servidor; en un contenedor o un servidor en UTC quedarían 6 horas adelantadas. Al arrancar, SIGIT fija la
 * zona de {@code sigit.zona-horaria} (America/Merida por omisión), sin depender de cómo esté el servidor.
 */
public class ZonaHoraria implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	public static final String PROPIEDAD = "sigit.zona-horaria";

	private static final Logger log = LoggerFactory.getLogger(ZonaHoraria.class);

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent evento) {
		String zona = evento.getEnvironment().getProperty(PROPIEDAD, "America/Merida");
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(zona)));
		log.info("Zona horaria de SIGIT: {}", zona);
	}

}
