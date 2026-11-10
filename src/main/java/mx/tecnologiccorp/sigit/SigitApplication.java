package mx.tecnologiccorp.sigit;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import mx.tecnologiccorp.sigit.config.ZonaHoraria;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class SigitApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder(SigitApplication.class).listeners(new ZonaHoraria()).run(args);
	}

}
