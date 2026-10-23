package com.ranen.ranen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RanenApplication {

	public static void main(String[] args) {
		SpringApplication.run(RanenApplication.class, args);
	}

}
