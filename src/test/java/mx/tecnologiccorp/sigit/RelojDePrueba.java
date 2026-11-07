package mx.tecnologiccorp.sigit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Reloj que las pruebas adelantan a voluntad. */
public class RelojDePrueba extends Clock {

	private Instant ahora = Instant.parse("2026-11-09T15:00:00Z");

	public void avanzar(Duration tiempo) {
		ahora = ahora.plus(tiempo);
	}

	public void reiniciar() {
		ahora = Instant.parse("2026-11-09T15:00:00Z");
	}

	@Override
	public ZoneId getZone() {
		return ZoneId.of("America/Merida");
	}

	@Override
	public Clock withZone(ZoneId zona) {
		return this;
	}

	@Override
	public Instant instant() {
		return ahora;
	}

	@TestConfiguration(proxyBeanMethods = false)
	public static class Config {

		@Bean
		@Primary
		RelojDePrueba relojDePrueba() {
			return new RelojDePrueba();
		}

	}

}
