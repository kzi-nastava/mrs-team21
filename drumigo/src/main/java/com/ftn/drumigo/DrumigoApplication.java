package com.ftn.drumigo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DrumigoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DrumigoApplication.class, args);
	}

}
