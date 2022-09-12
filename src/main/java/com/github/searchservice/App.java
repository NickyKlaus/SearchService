package com.github.searchservice;

import com.github.searchservice.configuration.DbConfiguration;
import com.github.searchservice.configuration.SecurityConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
@SpringBootConfiguration
@ImportAutoConfiguration({ DbConfiguration.class, SecurityConfiguration.class })
public class App {

    public static void main(String... args) {
        log.info("Application started");
        SpringApplication.run(App.class, args);
    }
}
