package org.example.brev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BrevApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrevApplication.class, args);
    }

}
