package com.br.criarcenariotestes.business.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuração global de CORS da aplicação (Fase 13.1B). Não pertence ao
 * módulo Auto QA — CorsConfig é global (mapping "/**") e afeta todos os
 * controllers (Auto QA, Agent/Chat IA, Jira, Cenários), por isso vive sob o
 * prefixo "app.cors", não "auto-qa".
 *
 * Fail-closed por padrão: lista vazia (default) significa que NENHUMA
 * origem cross-origin de browser é autorizada — nunca um fallback
 * implícito para "*"/localhost. Cada ambiente deve configurar
 * explicitamente suas origens legítimas (ex.: via variável de ambiente
 * APP_CORS_ALLOWED_ORIGINS, com múltiplas origens separadas por vírgula,
 * seguindo o binding relaxado padrão do Spring Boot).
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.cors")
public class AppCorsProperties {

    private List<String> allowedOrigins = new ArrayList<>();
}
