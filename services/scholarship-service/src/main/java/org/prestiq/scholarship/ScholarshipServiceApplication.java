package org.prestiq.scholarship;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ScholarshipServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScholarshipServiceApplication.class, args);
    }
}
