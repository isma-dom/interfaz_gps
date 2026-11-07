package mx.tecnologiccorp.sigit.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;

import mx.tecnologiccorp.sigit.PruebaIntegracion;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.Usuario;
import mx.tecnologiccorp.sigit.web.DatosDePrueba;

/**
 * Por HTTP estas reglas casi no se alcanzan: quien actúa ya es un administrador activo que no
 * puede quitarse el rol. Protegen del caso de dos administradores simultáneos.
 */
class UsuarioServiceTest extends PruebaIntegracion {

	@Autowired
	private UsuarioService servicio;

	@Autowired
	private TransactionTemplate transaccion;

	@Autowired
	private ApplicationContext contexto;

	@Test
	void noSeDesactivaAlUltimoAdministradorActivo() {
		Usuario ultimo = crearUsuario(Rol.ADMINISTRADOR);
		Usuario actor = crearUsuario(Rol.ADMINISTRADOR);
		actor.setActivo(false);
		usuarios.save(actor);

		assertThatThrownBy(() -> servicio.desactivar(ultimo.getId(), actor.getId()))
			.isInstanceOf(ErroresDeUsuario.class)
			.hasMessage("El sistema necesita al menos un administrador activo.");
		assertThat(usuarios.findById(ultimo.getId()).orElseThrow().isActivo()).isTrue();
	}

	@Test
	void noSeQuitaElRolAlUltimoAdministradorActivo() {
		Usuario ultimo = crearUsuario(Rol.ADMINISTRADOR);
		UsuarioForm formulario = UsuarioForm.de(ultimo);
		formulario.setRolId(rol(Rol.EJECUTIVO).getId());

		assertThatThrownBy(() -> servicio.actualizar(ultimo.getId(), formulario, -1L))
			.isInstanceOf(ErroresDeUsuario.class)
			.hasMessage("El sistema necesita al menos un administrador activo.");
	}

	/**
	 * Dos administradores intentan desactivarse entre sí al mismo tiempo. La primera
	 * transacción bloquea las filas; la segunda espera, ve el resultado y es rechazada.
	 */
	@Test
	void dosAdministradoresNoPuedenDesactivarseEntreSiAlMismoTiempo() throws Exception {
		Usuario a = crearUsuario(Rol.ADMINISTRADOR);
		Usuario b = crearUsuario(Rol.ADMINISTRADOR);
		CountDownLatch primeraConBloqueo = new CountDownLatch(1);
		CountDownLatch soltar = new CountDownLatch(1);

		CompletableFuture<Void> primera = CompletableFuture.runAsync(() -> transaccion.executeWithoutResult(estado -> {
			servicio.desactivar(b.getId(), a.getId());
			primeraConBloqueo.countDown();
			try {
				soltar.await(10, TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}));
		assertThat(primeraConBloqueo.await(10, TimeUnit.SECONDS)).isTrue();

		CompletableFuture<Void> segunda = CompletableFuture.runAsync(() -> servicio.desactivar(a.getId(), b.getId()));
		Thread.sleep(500);
		assertThat(segunda).as("la segunda espera al bloqueo").isNotDone();

		soltar.countDown();
		primera.get(10, TimeUnit.SECONDS);
		assertThatThrownBy(() -> segunda.get(10, TimeUnit.SECONDS))
			.hasCauseInstanceOf(ErroresDeUsuario.class);
		assertThat(usuarios.findAll().stream().filter(u -> u.isActivo() && u.tieneRol(Rol.ADMINISTRADOR))).hasSize(1);
	}

	@Test
	void desactivarAQuienNoEsAdministradorNoExigeOtroAdministrador() {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);

		servicio.desactivar(usuario.getId(), -1L);

		assertThat(usuarios.findById(usuario.getId()).orElseThrow().isActivo()).isFalse();
	}

	@Test
	void sinElPerfilDevNoSeCreanDatosDePrueba() {
		assertThat(contexto.getBeansOfType(DatosDePrueba.class)).isEmpty();
	}

}
