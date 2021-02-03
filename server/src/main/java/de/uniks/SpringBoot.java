package de.uniks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
public class SpringBoot extends SpringBootServletInitializer {

    public static void startSpring() {
        String[] args = {};
        try {
            SpringApplication.run(SpringBoot.class, args);
        } catch (Exception e) {
            java.lang.System.out.println("Failed to start Spring:");
            java.lang.System.out.println(e.getMessage());
        }
    }
}
