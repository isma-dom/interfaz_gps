package mx.tecnologiccorp.sigit.inventario;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import mx.tecnologiccorp.sigit.config.PropiedadesSigit;

/**
 * Tarea ClasificarObsolescencia (RN-06, AD 2.2.3): cada día pasa a Candidato a baja los equipos Operativos
 * o Inactivos que ya cumplieron su vida útil o cuyo sistema operativo perdió soporte. Es la puerta de
 * entrada a la baja: el Administrador solo da de baja equipos Candidatos a baja (o no localizados).
 */
@Component
public class ClasificadorDeObsolescencia {

	private static final Logger log = LoggerFactory.getLogger(ClasificadorDeObsolescencia.class);

	private final EquipoRepository equipos;

	private final MovimientoRepository movimientos;

	private final PropiedadesSigit.Obsolescencia reglas;

	private final Clock reloj;

	private final TransactionTemplate transaccion;

	public ClasificadorDeObsolescencia(EquipoRepository equipos, MovimientoRepository movimientos,
			PropiedadesSigit propiedades, Clock reloj, TransactionTemplate transaccion) {
		this.equipos = equipos;
		this.movimientos = movimientos;
		this.reglas = propiedades.obsolescencia();
		this.reloj = reloj;
		this.transaccion = transaccion;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void alArrancar() {
		clasificar();
	}

	/**
	 * Devuelve cuántos equipos pasaron a Candidato a baja. Todo en una transacción: el cambio de estado y
	 * su movimiento se guardan juntos o no se guarda ninguno.
	 */
	@Scheduled(cron = "${sigit.obsolescencia.cron}")
	public int clasificar() {
		Integer clasificados = transaccion.execute(estado -> clasificarEquipos());
		return clasificados == null ? 0 : clasificados;
	}

	private int clasificarEquipos() {
		LocalDate hoy = LocalDate.now(reloj);
		int clasificados = 0;
		for (Equipo equipo : equipos.findByEstadoIn(List.of(EstadoEquipo.OPERATIVO, EstadoEquipo.INACTIVO))) {
			Optional<String> motivo = motivo(equipo, hoy);
			if (motivo.isPresent()) {
				EstadoEquipo anterior = equipo.getEstado();
				equipo.cambiarEstado(EstadoEquipo.CANDIDATO_BAJA);
				movimientos.save(Movimiento.cambioDeEstado(equipo, null, anterior, motivo.get(),
						LocalDateTime.now(reloj)));
				clasificados++;
			}
		}
		if (clasificados > 0) {
			log.info("Clasificación de obsolescencia: {} equipos pasaron a Candidato a baja.", clasificados);
		}
		return clasificados;
	}

	/** Por qué el equipo es obsoleto, en palabras para el historial; vacío si no lo es. */
	Optional<String> motivo(Equipo equipo, LocalDate hoy) {
		if (equipo.getFechaAdquisicion() != null) {
			int anios = Period.between(equipo.getFechaAdquisicion(), hoy).getYears();
			if (anios >= reglas.anios()) {
				return Optional.of("Clasificación automática: tiene " + anios + " años de antigüedad (el límite es "
						+ reglas.anios() + ").");
			}
		}
		String so = equipo.getSistemaOperativo();
		if (so != null && reglas.soSinSoporte().stream().anyMatch(s -> s.trim().equalsIgnoreCase(so.trim()))) {
			return Optional.of("Clasificación automática: su sistema operativo (" + so + ") ya no tiene soporte.");
		}
		return Optional.empty();
	}

}
