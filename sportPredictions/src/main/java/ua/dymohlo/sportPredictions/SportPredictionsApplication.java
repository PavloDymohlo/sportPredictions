package ua.dymohlo.sportPredictions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SportPredictionsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SportPredictionsApplication.class, args);
	}

}
