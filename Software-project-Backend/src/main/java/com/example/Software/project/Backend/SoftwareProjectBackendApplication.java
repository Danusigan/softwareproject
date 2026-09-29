package com.example.Software.project.Backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SoftwareProjectBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SoftwareProjectBackendApplication.class, args);
	}

}
