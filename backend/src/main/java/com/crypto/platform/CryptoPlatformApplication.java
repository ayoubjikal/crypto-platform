package com.crypto.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CryptoPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CryptoPlatformApplication.class, args);
    }
}