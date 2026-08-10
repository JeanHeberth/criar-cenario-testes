package com.br.criarcenariotestes.business.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Objects;

/**
 * CORS global da aplicação (Fase 13.1B) — única fonte de política CORS,
 * cobrindo todos os controllers via mapping "/**". Origens vêm
 * exclusivamente de AppCorsProperties (nunca hardcoded aqui); lista vazia
 * (default) é fail-closed — nenhuma origem cross-origin é autorizada, sem
 * fallback para "*"/localhost implícito.
 */
@Configuration
public class CorsConfig {

    private final AppCorsProperties corsProperties;

    public CorsConfig(AppCorsProperties corsProperties) {
        this.corsProperties = Objects.requireNonNull(corsProperties, "corsProperties must not be null");
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(corsProperties.getAllowedOrigins().toArray(new String[0]))
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}