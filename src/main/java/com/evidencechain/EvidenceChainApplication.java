package com.evidencechain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EvidenceChainApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvidenceChainApplication.class, args);
    }
}
