package com.br.criarcenariotestes;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CriarCenarioTestesApplication {

    public static void main(String[] args) {

        SpringApplication.run(CriarCenarioTestesApplication.class, args);
    }

    @PostConstruct
    public void verificarVariaveis() {
        System.out.println("OPENAI_API_KEY existe? " + (System.getenv("OPENAI_API_KEY") != null));
        System.out.println("GEMINI_API_KEY existe? " + (System.getenv("GEMINI_API_KEY") != null));
        System.out.println("AI_ACTIVE_PROVIDER: " + System.getenv("AI_ACTIVE_PROVIDER"));
    }
}
