package com.ssafy.springbootbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringbootBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootBeApplication.class, args);
    }

}
