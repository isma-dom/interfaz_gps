package mx.tecnologiccorp.sigit;

import org.springframework.boot.SpringApplication;

public class TestSigitApplication {

	public static void main(String[] args) {
		SpringApplication.from(SigitApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
