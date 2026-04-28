package com.onboardguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OnboardguardBackendApplication {

	public static void main(String[] args) {

//		Dotenv dotenv = Dotenv.configure()
//				.ignoreIfMissing()
//				.load();
//
//		dotenv.entries().forEach(entry ->
//				System.setProperty(entry.getKey(), entry.getValue())
//		);

		SpringApplication.run(OnboardguardBackendApplication.class, args);
	}

}
