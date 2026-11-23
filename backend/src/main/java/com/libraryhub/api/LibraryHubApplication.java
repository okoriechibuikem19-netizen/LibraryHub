package com.libraryhub.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LibraryHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibraryHubApplication.class, args);
    }
}
