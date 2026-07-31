package com.br.criarcenariotestes.business.autoqa.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configurações do módulo Auto QA.
 * Todas as opções possuem valores padrão seguros.
 * <p>
 * Referência no application.yml:
 * <pre>
 * auto-qa:
 *   enabled: true
 *   allowed-roots: []
 *   max-files: 500
 *   max-file-size-kb: 500
 *   max-total-content-kb: 5000
 *   max-generation-retries: 3
 *   max-execution-minutes: 10
 *   generated-directory: .auto-qa/generated
 *   backup-directory: .auto-qa/backups
 *   allow-command-execution: false
 *   allow-file-application: false
 * </pre>
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "auto-qa")
public class AutoQaProperties {

    /** Habilita ou desabilita o módulo Auto QA inteiramente. */
    private boolean enabled = true;

    /**
     * Lista de raízes de diretórios permitidas.
     * Quando vazia, projetos locais são permitidos,
     * mas diretórios críticos continuam bloqueados.
     */
    private List<String> allowedRoots = List.of();

    /** Quantidade máxima de arquivos analisados no projeto. */
    private int maxFiles = 500;

    /** Tamanho máximo individual de arquivo em KB. */
    private int maxFileSizeKb = 500;

    /** Volume total máximo de conteúdo carregado em KB. */
    private int maxTotalContentKb = 5000;

    /** Número máximo de tentativas de geração/correção de código. */
    private int maxGenerationRetries = 3;

    /** Timeout máximo para execução de testes em minutos. */
    private int maxExecutionMinutes = 10;

    /** Diretório relativo ao projeto onde os arquivos gerados são salvos. */
    private String generatedDirectory = ".auto-qa/generated";

    /** Diretório relativo ao projeto onde os backups são salvos antes de sobrescrever. */
    private String backupDirectory = ".auto-qa/backups";

    /**
     * Habilita execução de comandos de teste.
     * Por segurança, false por padrão — deve ser habilitado explicitamente.
     */
    private boolean allowCommandExecution = false;

    /**
     * Habilita aplicação de arquivos gerados no projeto de automação.
     * Por segurança, false por padrão — deve ser habilitado explicitamente.
     */
    private boolean allowFileApplication = false;

    public boolean hasAllowedRoots() {
        return allowedRoots != null && !allowedRoots.isEmpty();
    }
}
