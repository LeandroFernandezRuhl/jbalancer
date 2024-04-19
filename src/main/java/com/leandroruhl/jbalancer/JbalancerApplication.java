package com.leandroruhl.jbalancer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JbalancerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbalancerApplication.class, args);
    }

}
