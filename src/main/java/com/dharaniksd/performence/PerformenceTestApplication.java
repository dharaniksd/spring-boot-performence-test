package com.dharaniksd.performence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PerformenceTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerformenceTestApplication.class, args);
    }
}
