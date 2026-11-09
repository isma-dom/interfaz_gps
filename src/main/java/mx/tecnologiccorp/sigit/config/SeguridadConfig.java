package mx.tecnologiccorp.sigit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import mx.tecnologiccorp.sigit.seguridad.FiltroUsuarioActivo;
import mx.tecnologiccorp.sigit.seguridad.ManejadorAccesoDenegado;
import mx.tecnologiccorp.sigit.seguridad.ManejadorExitoLogin;
import mx.tecnologiccorp.sigit.seguridad.ManejadorFalloLogin;
import mx.tecnologiccorp.sigit.seguridad.Pantalla;
import mx.tecnologiccorp.sigit.seguridad.Peticiones;
import mx.tecnologiccorp.sigit.seguridad.Rol;
import mx.tecnologiccorp.sigit.seguridad.UsuarioRepository;

/**
 * SS-01. El permiso de cada pantalla se valida aquí, en el servidor: ocultar una opción del
 * menú no es seguridad. Hace el papel del middleware VerificarRol (C-04) del diseño.
 */
@Configuration(proxyBeanMethods = false)
public class SeguridadConfig {

	@Bean
	PasswordEncoder cifradorDeContrasenas() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, ManejadorExitoLogin exito, ManejadorFalloLogin fallo, ManejadorAccesoDenegado accesoDenegado,
			UsuarioRepository usuarios) throws Exception {
		// ProveedorDeAutenticacion se registra solo por ser un bean. Agregarlo también aquí haría
		// que cada intento fallido contara doble (cadena local y AuthenticationManager global).
		http.authorizeHttpRequests(reglas -> {
				reglas.requestMatchers("/login", "/error", "/css/**", "/js/**", "/fuentes/**", "/webjars/**")
					.permitAll();
				// Registrar, editar y dar de baja equipos es solo del Administrador (RF-02, RN-04); consultar es
				// de todos los roles.
				reglas.requestMatchers("/equipos/nuevo", "/equipos/*/editar", "/equipos/*/baja")
					.hasRole(Rol.ADMINISTRADOR);
				reglas.requestMatchers(HttpMethod.POST, "/equipos", "/equipos/**").hasRole(Rol.ADMINISTRADOR);
				// Prototipo de la Tarea 1 (solo perfil dev): registrar y editar equipos es del Administrador.
				reglas.requestMatchers("/prototipos/equipos/nuevo", "/prototipos/equipos/*/editar")
					.hasRole(Rol.ADMINISTRADOR);
				reglas.requestMatchers(HttpMethod.POST, "/prototipos/equipos/**").hasRole(Rol.ADMINISTRADOR);
				for (Pantalla pantalla : Pantalla.values()) {
					reglas.requestMatchers(pantalla.getRuta(), pantalla.getRuta() + "/**")
						.hasAnyRole(pantalla.getRoles().toArray(String[]::new));
				}
				reglas.anyRequest().authenticated();
			})
			.formLogin(login -> login.loginPage("/login")
				.usernameParameter("correo")
				.passwordParameter("password")
				.successHandler(exito)
				.failureHandler(fallo)
				.permitAll())
			.logout(salida -> salida.logoutUrl("/logout")
				.logoutSuccessUrl("/login")
				.invalidateHttpSession(true)
				.deleteCookies("JSESSIONID"))
			.exceptionHandling(errores -> errores.accessDeniedHandler(accesoDenegado)
				.authenticationEntryPoint(sinSesion()))
			.addFilterBefore(new FiltroUsuarioActivo(usuarios), AuthorizationFilter.class);
		return http.build();
	}

	/** Sin sesión: las páginas llevan al login y las peticiones JSON reciben 401. */
	private static AuthenticationEntryPoint sinSesion() {
		AuthenticationEntryPoint login = new LoginUrlAuthenticationEntryPoint("/login");
		AuthenticationEntryPoint json = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
		return (peticion, respuesta, error) -> (Peticiones.esperaJson(peticion) ? json : login).commence(peticion,
				respuesta, error);
	}

}
