package mx.tecnologiccorp.sigit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SigitApplication {

	public static void main(String[] args) {
		SpringApplication.run(SigitApplication.class, args);
	}

}
