package com.secura.dnft;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.secura.dnft", "com.secura.access"})
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	
	@PostConstruct
    public void init() {
        // Setting Spring Boot SetTimeZone to IST
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        //TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
        System.out.println("Spring Boot application configured to Asia/Kolkata timezone.");
    }
}
