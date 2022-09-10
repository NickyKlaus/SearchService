package com.github.searchservice;

import com.github.searchservice.configuration.DbConfiguration;
import com.github.searchservice.configuration.SecurityConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@SpringBootConfiguration
@ImportAutoConfiguration({ DbConfiguration.class, SecurityConfiguration.class })
public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String... args) {
        LOGGER.info("Application started");
        SpringApplication.run(App.class, args);
    }
}
