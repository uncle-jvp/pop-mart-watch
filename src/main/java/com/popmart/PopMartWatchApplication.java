package com.popmart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PopMartWatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(PopMartWatchApplication.class, args);
    }
} 