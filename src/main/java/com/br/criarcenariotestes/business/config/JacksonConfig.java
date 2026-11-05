package com.br.criarcenariotestes.business.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O Spring Boot 4 passou a auto-configurar apenas o Jackson 3
 * (tools.jackson). O código da aplicação ainda usa o Jackson 2
 * (com.fasterxml), então o ObjectMapper dele é exposto aqui.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
