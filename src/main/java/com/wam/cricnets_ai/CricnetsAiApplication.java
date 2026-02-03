package com.wam.cricnets_ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = ManagementWebSecurityAutoConfiguration.class)
public class CricnetsAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CricnetsAiApplication.class, args);
	}

}
