package mx.tecnologiccorp.sigit.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import mx.tecnologiccorp.sigit.PruebaIntegracion;

/** CP-03: cinco intentos fallidos con la misma cuenta la bloquean temporalmente. */
class BloqueoDeCuentaTest extends PruebaIntegracion {

	private static final String BLOQUEADA = "Demasiados intentos fallidos. Intenta de nuevo en 15 minutos.";

	private void fallar(String correo, int veces, String ip) throws Exception {
		for (int i = 0; i < veces; i++) {
			mvc.perform(post("/login").with(csrf()).param("correo", correo).param("password", "incorrecta")
				.with(peticion -> {
					peticion.setRemoteAddr(ip);
					return peticion;
				}));
		}
	}

	private void fallar(String correo, int veces) throws Exception {
		fallar(correo, veces, "10.0.0.1");
	}

	private boolean entra(String correo) throws Exception {
		return "/tablero".equals(entrar(correo, CLAVE).getResponse().getRedirectedUrl());
	}

	@Test
	void cincoIntentosFallidosBloqueanLaCuenta() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);

		fallar(usuario.getCorreo(), 4);
		MvcResult quinto = entrar(usuario.getCorreo(), "incorrecta");

		assertThat(loginTras(quinto)).contains(BLOQUEADA);
		MvcResult correcta = entrar(usuario.getCorreo(), CLAVE);
		assertThat(correcta.getResponse().getRedirectedUrl()).isEqualTo("/login");
		assertThat(loginTras(correcta)).contains("Demasiados intentos fallidos.");
	}

	@Test
	void cuatroIntentosFallidosNoBloquean() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);

		fallar(usuario.getCorreo(), 4);

		assertThat(entra(usuario.getCorreo())).isTrue();
	}

	@Test
	void elBloqueoAlcanzaALaCuentaDesdeOtraIp() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);

		fallar(usuario.getCorreo(), 3, "10.0.0.1");
		fallar(usuario.getCorreo(), 2, "10.0.0.2");

		assertThat(entra(usuario.getCorreo())).isFalse();
	}

	@Test
	void cambiarMayusculasDelCorreoNoEvitaElBloqueo() throws Exception {
		crearUsuario(Rol.EJECUTIVO, "ana@sigit.test");

		fallar("ana@sigit.test", 3);
		fallar("ANA@sigit.test", 2);

		assertThat(entra("ana@sigit.test")).isFalse();
	}

	@Test
	void elBloqueoTerminaAlVencerElPlazo() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);
		fallar(usuario.getCorreo(), 5);

		reloj.avanzar(Duration.ofMinutes(14));
		assertThat(loginTras(entrar(usuario.getCorreo(), CLAVE))).contains("Intenta de nuevo en 1 minuto.");

		reloj.avanzar(Duration.ofSeconds(61));
		assertThat(entra(usuario.getCorreo())).isTrue();
	}

	@Test
	void elPlazoCuentaDesdeElUltimoIntento() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);

		fallar(usuario.getCorreo(), 4);
		reloj.avanzar(Duration.ofMinutes(14));
		fallar(usuario.getCorreo(), 1);
		reloj.avanzar(Duration.ofMinutes(14));

		assertThat(entra(usuario.getCorreo())).isFalse();
	}

	@Test
	void entrarReiniciaElConteo() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);

		fallar(usuario.getCorreo(), 4);
		assertThat(entra(usuario.getCorreo())).isTrue();
		fallar(usuario.getCorreo(), 4);

		assertThat(entra(usuario.getCorreo())).isTrue();
	}

	@Test
	void elBloqueoDeUnaCuentaNoAfectaAOtra() throws Exception {
		Usuario bloqueado = crearUsuario(Rol.EJECUTIVO);
		Usuario otro = crearUsuario(Rol.EJECUTIVO);

		fallar(bloqueado.getCorreo(), 5);

		assertThat(entra(otro.getCorreo())).isTrue();
	}

	@Test
	void unCorreoInexistenteSeBloqueaIgualParaNoRevelarCuentas() throws Exception {
		fallar("nadie@sigit.test", 4);

		assertThat(loginTras(entrar("nadie@sigit.test", "incorrecta"))).contains(BLOQUEADA);
	}

	@Test
	void losCamposVaciosNoCuentanComoIntento() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);

		for (int i = 0; i < 5; i++) {
			entrar(usuario.getCorreo(), "");
		}

		assertThat(entra(usuario.getCorreo())).isTrue();
	}

}
