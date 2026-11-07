package mx.tecnologiccorp.sigit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import mx.tecnologiccorp.sigit.catalogos.Area;
import mx.tecnologiccorp.sigit.catalogos.AreaRepository;
import mx.tecnologiccorp.sigit.seguridad.BloqueoDeAcceso;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.RolRepository;
import mx.tecnologiccorp.sigit.seguridad.Usuario;
import mx.tecnologiccorp.sigit.seguridad.UsuarioRepository;
import mx.tecnologiccorp.sigit.seguridad.UsuarioSesion;

/** Base de las pruebas: contexto completo, MockMvc y MySQL 8.4 en Testcontainers. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, RelojDePrueba.Config.class })
public abstract class PruebaIntegracion {

	protected static final String CLAVE = "Clave1234";

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected UsuarioRepository usuarios;

	@Autowired
	protected RolRepository roles;

	@Autowired
	protected AreaRepository areas;

	@Autowired
	protected PasswordEncoder cifrador;

	@Autowired
	protected BloqueoDeAcceso bloqueo;

	@Autowired
	protected RelojDePrueba reloj;

	private int consecutivo;

	@BeforeEach
	void limpiarDatos() {
		usuarios.deleteAll();
		areas.deleteAll();
		bloqueo.limpiarTodo();
		reloj.reiniciar();
	}

	protected Rol rol(String nombre) {
		return roles.findByNombre(nombre).orElseThrow();
	}

	protected Area crearArea(String nombre) {
		return areas.save(new Area(nombre, "Edificio principal", "1"));
	}

	protected Usuario crearUsuario(String rol, String correo) {
		Area area = Rol.CONSULTA.equals(rol) ? crearArea("Área " + (++consecutivo) + " " + correo) : null;
		return usuarios.save(new Usuario("Persona " + correo, correo, cifrador.encode(CLAVE), rol(rol), area));
	}

	protected Usuario crearUsuario(String rol) {
		return crearUsuario(rol, rol.toLowerCase() + (++consecutivo) + "@sigit.test");
	}

	/** Sesión ya autenticada, sin pasar por el formulario. */
	protected RequestPostProcessor como(Usuario usuario) {
		UsuarioSesion sesion = UsuarioSesion.de(usuario);
		return authentication(UsernamePasswordAuthenticationToken.authenticated(sesion, null, sesion.permisos()));
	}

	protected MvcResult entrar(String correo, String password) throws Exception {
		return mvc.perform(formLogin("/login").userParameter("correo").user(correo).password(password)).andReturn();
	}

	protected MockHttpSession sesionDe(MvcResult resultado) {
		return (MockHttpSession) resultado.getRequest().getSession(false);
	}

	/** Página de login que ve la persona tras la redirección, con los mensajes de un solo uso. */
	protected String loginTras(MvcResult resultado) throws Exception {
		return mvc.perform(get("/login").session(sesionDe(resultado))).andReturn().getResponse()
			.getContentAsString();
	}

}
