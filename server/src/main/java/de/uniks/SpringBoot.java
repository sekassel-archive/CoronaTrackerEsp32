package de.uniks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.util.logging.Level;
import java.util.logging.Logger;


@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class SpringBoot extends SpringBootServletInitializer {

    private static final Logger LOG = Logger.getLogger(SpringBoot.class.getName());

    public static void startSpring() {
        String[] args = {};
        try {
            SpringApplication.run(SpringBoot.class, args);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to start Spring!", e);
        }
    }
}
