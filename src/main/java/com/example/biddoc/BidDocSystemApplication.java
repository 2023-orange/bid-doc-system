package com.example.biddoc;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BidDocSystemApplication implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication.run(BidDocSystemApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("hello");
    }
}
