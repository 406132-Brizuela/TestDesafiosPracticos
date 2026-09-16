package com.tp.desafiospracticos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DesafiosPracticosApplication {

    public static void main(String[] args) {
        SpringApplication.run(DesafiosPracticosApplication.class, args);
    }

}
