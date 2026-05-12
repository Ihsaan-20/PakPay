package com.example.pakpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class PakpayApplication {

	public static void main(String[] args) {
		SpringApplication.run(PakpayApplication.class, args);
	}

}
