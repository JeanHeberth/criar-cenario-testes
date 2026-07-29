package com.br.criarcenariotestes.business.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ai.openai")
public class OpenAiProperties {

    private String apiKey;
    private String model;
    private String url;
    private Integer maxTokens = 4000;
}