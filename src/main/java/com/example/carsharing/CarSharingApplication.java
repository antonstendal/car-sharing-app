package com.example.carsharing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CarSharingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarSharingApplication.class, args);
    }

}
