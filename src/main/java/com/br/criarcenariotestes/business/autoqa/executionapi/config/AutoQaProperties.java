package com.br.criarcenariotestes.business.autoqa.executionapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuração tipada do módulo Auto QA, conectando o bloco `auto-qa:` do
 * application.yml (antes desconectado do código). Valores seguros por
 * padrão: ações sensíveis (aplicação de arquivo e execução de comando) só
 * ficam disponíveis quando explicitamente habilitadas.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "auto-qa")
public class AutoQaProperties {

    private boolean enabled = true;
    private List<String> allowedRoots = new ArrayList<>();
    private int maxFiles = 500;
    private int maxFileSizeKb = 500;
    private int maxTotalContentKb = 5000;
    private int maxGenerationRetries = 3;
    private int maxExecutionMinutes = 10;
    private String generatedDirectory = ".auto-qa/generated";
    private String backupDirectory = ".auto-qa/backups";
    private boolean allowCommandExecution = false;
    private boolean allowFileApplication = false;
    private boolean sensitiveActionsEnabled = false;
    private int retentionDays = 30;
    private int maxConcurrentExecutions = 5;
}
