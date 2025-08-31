package digital.alf.cells;

import org.springframework.boot.SpringApplication;

public class TestCustomsbsApplication {

	public static void main(String[] args) {
		SpringApplication.from(CustomsbsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
