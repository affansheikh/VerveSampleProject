package com.verve.VerveSampleProject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VerveSampleProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(VerveSampleProjectApplication.class, args);
	}

}
