package com.armando0405.tuboletascraper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TuBoletaScraperApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TuBoletaScraperApiApplication.class, args);
    }

}
