package com.ibeanny.aisorter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AisorterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AisorterApplication.class, args);
    }

}
