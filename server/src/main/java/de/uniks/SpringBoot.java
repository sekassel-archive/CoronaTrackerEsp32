package de.uniks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

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
