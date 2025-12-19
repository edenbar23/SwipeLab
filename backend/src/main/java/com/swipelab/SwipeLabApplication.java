package com.swipelab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SwipeLabApplication {
	public static void main(String[] args) {
		SpringApplication.run(SwipeLabApplication.class, args);
	}

}