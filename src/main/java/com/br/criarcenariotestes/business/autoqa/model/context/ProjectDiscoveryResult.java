package com.br.criarcenariotestes.business.autoqa.model.context;

import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.PackageManager;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Resultado da etapa de descoberta do projeto de automação.
 * Contém evidências determinísticas coletadas sem uso de IA.
 */
@Getter
@Builder
public class ProjectDiscoveryResult {

    /** Framework informado pelo usuário na request. */
    private final AutomationFramework informedFramework;

    /** Framework detectado deterministicamente pelos arquivos do projeto. */
    private final AutomationFramework detectedFramework;

    /** Linguagem informada pelo usuário. */
    private final AutomationLanguage informedLanguage;

    /** Linguagem detectada deterministicamente. */
    private final AutomationLanguage detectedLanguage;

    /** Gerenciador de pacotes detectado pelo arquivo de lock. */
    private final PackageManager packageManager;

    /** Arquivo de configuração principal do framework encontrado. */
    private final String configurationFile;

    /** Comandos sugeridos pelo adapter do framework detectado. */
    private final List<AllowedCommand> suggestedCommands;

    /** Evidências usadas para detecção de framework e linguagem. */
    private final List<String> detectionEvidences;

    /**
     * Divergências encontradas (ex: framework informado != detectado).
     * Se não vazio, o workflow não deve avançar automaticamente.
     */
    private final List<String> divergences;

    /** Avisos não bloqueantes. */
    private final List<String> warnings;

    /**
     * Indica se há divergência entre o framework informado e o detectado.
     * O workflow deve pausar e aguardar confirmação do usuário nesse caso.
     */
    public boolean hasFrameworkDivergence() {
        return informedFramework != null
                && informedFramework != AutomationFramework.UNKNOWN
                && detectedFramework != AutomationFramework.UNKNOWN
                && informedFramework != detectedFramework;
    }

    /**
     * Retorna o framework efetivo a ser usado: detectado tem prioridade.
     * Se nenhum foi detectado, usa o informado.
     */
    public AutomationFramework effectiveFramework() {
        if (detectedFramework != null && detectedFramework != AutomationFramework.UNKNOWN) {
            return detectedFramework;
        }
        return informedFramework != null ? informedFramework : AutomationFramework.UNKNOWN;
    }

    /**
     * Retorna a linguagem efetiva a ser usada.
     */
    public AutomationLanguage effectiveLanguage() {
        if (detectedLanguage != null && detectedLanguage != AutomationLanguage.UNKNOWN) {
            return detectedLanguage;
        }
        return informedLanguage != null ? informedLanguage : AutomationLanguage.UNKNOWN;
    }
}
