package mx.tecnologiccorp.sigit.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import mx.tecnologiccorp.sigit.PruebaIntegracion;
import mx.tecnologiccorp.sigit.catalogos.Area;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.Usuario;

class UsuariosTest extends PruebaIntegracion {

	private Usuario administrador;

	@BeforeEach
	void crearAdministrador() {
		administrador = crearUsuario(Rol.ADMINISTRADOR, "admin@sigit.test");
	}

	private MockHttpServletRequestBuilder formulario(String url, String nombre, String correo, String rol, Long area,
			String password) {
		MockHttpServletRequestBuilder peticion = post(url).with(csrf())
			.param("nombre", nombre)
			.param("correo", correo)
			.param("rolId", rol == null ? "" : String.valueOf(rol(rol).getId()))
			.param("password", password);
		return area == null ? peticion.param("areaId", "") : peticion.param("areaId", String.valueOf(area));
	}

	private ResultActions agregar(String nombre, String correo, String rol, Long area, String password)
			throws Exception {
		return mvc.perform(formulario("/usuarios", nombre, correo, rol, area, password).with(como(administrador)));
	}

	private ResultActions editar(Usuario usuario, String nombre, String correo, String rol, Long area,
			String password) throws Exception {
		return mvc.perform(formulario("/usuarios/" + usuario.getId(), nombre, correo, rol, area, password)
			.with(como(administrador)));
	}

	@Test
	void soloElAdministradorGestionaUsuarios() throws Exception {
		Usuario objetivo = crearUsuario(Rol.EJECUTIVO);

		for (String rol : new String[] { Rol.EJECUTIVO, Rol.CONSULTA }) {
			Usuario actor = crearUsuario(rol);
			mvc.perform(get("/usuarios").with(como(actor))).andExpect(status().isForbidden());
			mvc.perform(get("/usuarios/nuevo").with(como(actor))).andExpect(status().isForbidden());
			mvc.perform(formulario("/usuarios", "Ana", "ana@sigit.test", Rol.EJECUTIVO, null, "Clave1234")
				.with(como(actor))).andExpect(status().isForbidden());
			mvc.perform(get("/usuarios/" + objetivo.getId() + "/editar").with(como(actor)))
				.andExpect(status().isForbidden());
			mvc.perform(post("/usuarios/" + objetivo.getId() + "/desactivar").with(csrf()).with(como(actor)))
				.andExpect(status().isForbidden());
		}

		assertThat(usuarios.findByCorreo("ana@sigit.test")).isEmpty();
		assertThat(usuarios.findById(objetivo.getId()).orElseThrow().isActivo()).isTrue();
	}

	@Test
	void agregarUsuarioGuardaLaContrasenaCifrada() throws Exception {
		agregar("Ana Canul", "ana@sigit.test", Rol.EJECUTIVO, null, "Clave1234")
			.andExpect(redirectedUrl("/usuarios"))
			.andExpect(flash().attribute("exito", "Usuario Ana Canul agregado."));

		Usuario usuario = usuarios.findByCorreo("ana@sigit.test").orElseThrow();
		assertThat(cifrador.matches("Clave1234", usuario.getPassword())).isTrue();
		assertThat(usuario.getPassword()).doesNotContain("Clave1234");
		assertThat(usuario.isActivo()).isTrue();
	}

	@Test
	void losAcentosSeGuardanEnMysql() throws Exception {
		agregar("Rosa Canché Ñuñez", "rosa@sigit.test", Rol.EJECUTIVO, null, "Clave1234");

		assertThat(usuarios.findByCorreo("rosa@sigit.test").orElseThrow().getNombre()).isEqualTo("Rosa Canché Ñuñez");
	}

	@Test
	void correoDuplicadoDaError() throws Exception {
		crearUsuario(Rol.EJECUTIVO, "ana@sigit.test");

		agregar("Otra", "ANA@sigit.test", Rol.EJECUTIVO, null, "Clave1234")
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Ya existe un usuario con este correo.")));

		assertThat(usuarios.findAll().stream().filter(u -> u.getCorreo().equals("ana@sigit.test"))).hasSize(1);
	}

	@Test
	void camposObligatoriosYCorreoInvalido() throws Exception {
		agregar("", "no-es-correo", null, null, "")
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Escribe el nombre.")))
			.andExpect(content().string(containsString("Escribe un correo válido")))
			.andExpect(content().string(containsString("Elige un rol.")));
	}

	@Test
	void areaObligatoriaSoloEnConsulta() throws Exception {
		agregar("Ana", "ana@sigit.test", Rol.CONSULTA, null, "Clave1234")
			.andExpect(content().string(containsString("Elige el área de este usuario.")));

		Area area = crearArea("Sala de consulta");
		agregar("Ana", "ana@sigit.test", Rol.CONSULTA, area.getId(), "Clave1234").andExpect(redirectedUrl("/usuarios"));

		assertThat(usuarios.findByCorreo("ana@sigit.test").orElseThrow().getArea().getId()).isEqualTo(area.getId());
	}

	@Test
	void elAreaSeGuardaNulaSiElRolNoEsConsulta() throws Exception {
		Area area = crearArea("Sala de consulta");

		agregar("Ana", "ana@sigit.test", Rol.EJECUTIVO, area.getId(), "Clave1234").andExpect(redirectedUrl("/usuarios"));

		assertThat(usuarios.findByCorreo("ana@sigit.test").orElseThrow().getArea()).isNull();
	}

	@Test
	void cambiarDeConsultaAOtroRolBorraElArea() throws Exception {
		Usuario usuario = crearUsuario(Rol.CONSULTA);

		editar(usuario, usuario.getNombre(), usuario.getCorreo(), Rol.EJECUTIVO, usuario.getArea().getId(), "")
			.andExpect(redirectedUrl("/usuarios"));

		assertThat(usuarios.findById(usuario.getId()).orElseThrow().getArea()).isNull();
	}

	@Test
	void politicaDeContrasena() throws Exception {
		for (String invalida : new String[] { "", "Corta1", "SoloLetras", "12345678" }) {
			agregar("Ana", "ana@sigit.test", Rol.EJECUTIVO, null, invalida).andExpect(status().isOk());
		}
		agregar("Ana", "ana@sigit.test", Rol.EJECUTIVO, null, "SoloLetras")
			.andExpect(content().string(containsString("al menos 8 caracteres, con letras y números")));
		agregar("Ana", "ana@sigit.test", Rol.EJECUTIVO, null, "a1".repeat(37))
			.andExpect(content().string(containsString("no debe tener más de 72 caracteres")));

		assertThat(usuarios.findByCorreo("ana@sigit.test")).isEmpty();
	}

	@Test
	void contrasenaVaciaEnEdicionNoLaCambia() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);
		String anterior = usuario.getPassword();

		editar(usuario, "Ana Canul", usuario.getCorreo(), Rol.EJECUTIVO, null, "").andExpect(redirectedUrl("/usuarios"));

		Usuario guardado = usuarios.findById(usuario.getId()).orElseThrow();
		assertThat(guardado.getPassword()).isEqualTo(anterior);
		assertThat(guardado.getNombre()).isEqualTo("Ana Canul");
	}

	@Test
	void contrasenaNuevaEnEdicionLaCambia() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);

		editar(usuario, usuario.getNombre(), usuario.getCorreo(), Rol.EJECUTIVO, null, "Nueva1234")
			.andExpect(redirectedUrl("/usuarios"));

		assertThat(cifrador.matches("Nueva1234", usuarios.findById(usuario.getId()).orElseThrow().getPassword()))
			.isTrue();
	}

	@Test
	void desactivarYReactivar() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO, "luis@sigit.test");
		usuario.setNombre("Luis Pech");
		usuarios.save(usuario);

		mvc.perform(post("/usuarios/" + usuario.getId() + "/desactivar").with(csrf()).with(como(administrador)))
			.andExpect(redirectedUrl("/usuarios"))
			.andExpect(flash().attribute("exito", "Usuario Luis Pech desactivado."));
		assertThat(usuarios.findById(usuario.getId()).orElseThrow().isActivo()).isFalse();

		mvc.perform(post("/usuarios/" + usuario.getId() + "/reactivar").with(csrf()).with(como(administrador)))
			.andExpect(flash().attribute("exito", "Usuario Luis Pech reactivado."));
		assertThat(usuarios.findById(usuario.getId()).orElseThrow().isActivo()).isTrue();
	}

	@Test
	void noExisteRutaDeEliminacion() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);

		mvc.perform(delete("/usuarios/" + usuario.getId()).with(csrf()).with(como(administrador)))
			.andExpect(status().isMethodNotAllowed());
		assertThat(usuarios.existsById(usuario.getId())).isTrue();
	}

	@Test
	void noPuedesDesactivarteATiMismo() throws Exception {
		crearUsuario(Rol.ADMINISTRADOR);

		mvc.perform(post("/usuarios/" + administrador.getId() + "/desactivar").with(csrf()).with(como(administrador)))
			.andExpect(flash().attribute("error", "No puedes desactivar tu propia cuenta."));

		assertThat(usuarios.findById(administrador.getId()).orElseThrow().isActivo()).isTrue();
	}

	@Test
	void noPuedesQuitarteElRolDeAdministrador() throws Exception {
		crearUsuario(Rol.ADMINISTRADOR);

		editar(administrador, administrador.getNombre(), administrador.getCorreo(), Rol.EJECUTIVO, null, "")
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("No puedes quitarte el rol de Administrador.")));

		assertThat(usuarios.findById(administrador.getId()).orElseThrow().tieneRol(Rol.ADMINISTRADOR)).isTrue();
	}

	@Test
	void puedesEditarTusDatosConservandoTuRol() throws Exception {
		editar(administrador, "Ana Canul", administrador.getCorreo(), Rol.ADMINISTRADOR, null, "")
			.andExpect(redirectedUrl("/usuarios"));

		assertThat(usuarios.findById(administrador.getId()).orElseThrow().getNombre()).isEqualTo("Ana Canul");
	}

	@Test
	void unAdministradorPuedeDesactivarAOtroSiQuedaUno() throws Exception {
		Usuario otro = crearUsuario(Rol.ADMINISTRADOR);

		mvc.perform(post("/usuarios/" + otro.getId() + "/desactivar").with(csrf()).with(como(administrador)))
			.andExpect(flash().attribute("exito", "Usuario " + otro.getNombre() + " desactivado."));

		assertThat(usuarios.findById(otro.getId()).orElseThrow().isActivo()).isFalse();
	}

	@Test
	void laListaBuscaPorNombreOCorreoYFiltraPorRol() throws Exception {
		Usuario marta = crearUsuario(Rol.EJECUTIVO, "marta@sigit.test");
		marta.setNombre("Marta Euan");
		usuarios.save(marta);
		Usuario pedro = crearUsuario(Rol.CONSULTA, "pedro@sigit.test");
		pedro.setNombre("Pedro Dzul");
		usuarios.save(pedro);

		mvc.perform(get("/usuarios?q=marta").with(como(administrador)))
			.andExpect(content().string(containsString("Marta Euan")))
			.andExpect(content().string(not(containsString("Pedro Dzul"))));
		mvc.perform(get("/usuarios?q=pedro@").with(como(administrador)))
			.andExpect(content().string(containsString("Pedro Dzul")))
			.andExpect(content().string(not(containsString("Marta Euan"))));
		mvc.perform(get("/usuarios?rol=" + rol(Rol.CONSULTA).getId()).with(como(administrador)))
			.andExpect(content().string(containsString("Pedro Dzul")))
			.andExpect(content().string(not(containsString("Marta Euan"))));
		mvc.perform(get("/usuarios?q=zzz").with(como(administrador)))
			.andExpect(content().string(containsString("Ningún usuario coincide con la búsqueda")));
	}

	@Test
	void losComodinesDeBusquedaSeTratanComoTexto() throws Exception {
		mvc.perform(get("/usuarios?q=%25").with(como(administrador)))
			.andExpect(content().string(containsString("Ningún usuario coincide con la búsqueda")));
		mvc.perform(get("/usuarios?q=_").with(como(administrador)))
			.andExpect(content().string(containsString("Ningún usuario coincide con la búsqueda")));
		mvc.perform(get("/usuarios?q=!").with(como(administrador)))
			.andExpect(content().string(containsString("Ningún usuario coincide con la búsqueda")));
	}

	@Test
	void laListaEstaPaginadaYConservaLosFiltros() throws Exception {
		for (int i = 0; i < 20; i++) {
			Usuario usuario = crearUsuario(Rol.EJECUTIVO);
			usuario.setNombre("Persona " + i);
			usuarios.save(usuario);
		}

		mvc.perform(get("/usuarios?q=Persona").with(como(administrador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("/usuarios?q=Persona")))
			.andExpect(content().string(containsString("pagina=2\">2<")));
		mvc.perform(get("/usuarios?q=Persona&pagina=2").with(como(administrador)))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("aria-current=\"page\">2<")));
	}

	@Test
	void losNombresSeEscapanEnLaLista() throws Exception {
		Usuario usuario = crearUsuario(Rol.EJECUTIVO);
		usuario.setNombre("<script>alert(1)</script>");
		usuarios.save(usuario);

		mvc.perform(get("/usuarios").with(como(administrador)))
			.andExpect(status().isOk())
			.andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
	}

	@Test
	void tuPropiaFilaNoOfreceDesactivar() throws Exception {
		mvc.perform(get("/usuarios").with(como(administrador)))
			.andExpect(content().string(containsString(">Tú<")))
			.andExpect(content().string(not(containsString("/usuarios/" + administrador.getId() + "/desactivar"))));
	}

	@Test
	void editarUnUsuarioInexistenteDa404() throws Exception {
		mvc.perform(get("/usuarios/999999/editar").with(como(administrador))).andExpect(status().isNotFound());
	}

}
