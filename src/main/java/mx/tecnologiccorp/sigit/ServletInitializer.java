package mx.tecnologiccorp.sigit;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/** Punto de entrada al desplegar sigit.war en un Apache Tomcat 11 externo. */
public class ServletInitializer extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(SigitApplication.class);
	}

}
