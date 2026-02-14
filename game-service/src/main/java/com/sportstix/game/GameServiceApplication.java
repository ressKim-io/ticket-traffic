package com.sportstix.game;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(
        title = "Game Service API",
        description = "Stadium, section, seat, and game management",
        version = "1.0.0"
))
@SpringBootApplication
public class GameServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GameServiceApplication.class, args);
    }
}
