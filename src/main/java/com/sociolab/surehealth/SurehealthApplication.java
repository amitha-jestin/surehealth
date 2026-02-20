package com.sociolab.surehealth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SurehealthApplication {

	public static void main(String[] args) {
		SpringApplication.run(SurehealthApplication.class, args);
	}

}
