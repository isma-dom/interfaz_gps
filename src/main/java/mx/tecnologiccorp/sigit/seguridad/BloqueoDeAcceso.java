package mx.tecnologiccorp.sigit.seguridad;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import mx.tecnologiccorp.sigit.config.PropiedadesSigit;

/**
 * Bloqueo temporal tras 5 intentos fallidos con la misma cuenta (C-01, CP-03).
 *
 * <p>La clave es solo el correo normalizado, sin la IP: cambiar de equipo no reinicia la
 * cuenta. Se comporta igual exista o no la cuenta, para no revelar qué correos están
 * registrados. El estado vive en memoria: SIGIT corre en un solo servidor y un reinicio
 * solo levanta los bloqueos vigentes.
 */
@Component
public class BloqueoDeAcceso {

	private static final int LIMITE_ANTES_DE_PURGAR = 10_000;

	private final Map<String, Estado> estados = new ConcurrentHashMap<>();

	private final Clock reloj;

	private final int intentos;

	private final Duration duracion;

	public BloqueoDeAcceso(Clock reloj, PropiedadesSigit propiedades) {
		this.reloj = reloj;
		this.intentos = propiedades.bloqueo().intentos();
		this.duracion = Duration.ofMinutes(propiedades.bloqueo().minutos());
	}

	public boolean estaBloqueado(String correo) {
		Estado estado = estados.get(correo);
		return estado != null && estado.bloqueadoHasta() != null && reloj.instant().isBefore(estado.bloqueadoHasta());
	}

	public long minutosRestantes(String correo) {
		Estado estado = estados.get(correo);
		if (estado == null || estado.bloqueadoHasta() == null) {
			return 0;
		}
		long segundos = Duration.between(reloj.instant(), estado.bloqueadoHasta()).toSeconds();
		return Math.max(1, (segundos + 59) / 60);
	}

	/** El plazo del bloqueo cuenta desde el último intento fallido, no desde el primero. */
	public void registrarFallo(String correo) {
		Instant ahora = reloj.instant();
		Instant vence = ahora.plus(duracion);
		estados.compute(correo, (clave, anterior) -> {
			boolean vigente = anterior != null && ahora.isBefore(anterior.venceEn());
			int fallos = vigente ? anterior.fallos() + 1 : 1;
			return fallos >= intentos ? new Estado(0, vence, vence) : new Estado(fallos, vence, null);
		});
		if (estados.size() > LIMITE_ANTES_DE_PURGAR) {
			estados.values().removeIf(estado -> !ahora.isBefore(estado.venceEn()));
		}
	}

	public void limpiar(String correo) {
		estados.remove(correo);
	}

	/** Solo para pruebas. */
	public void limpiarTodo() {
		estados.clear();
	}

	private record Estado(int fallos, Instant venceEn, Instant bloqueadoHasta) {
	}

}
