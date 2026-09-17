package com.coview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CoViewApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoViewApplication.class, args);
    }
}
