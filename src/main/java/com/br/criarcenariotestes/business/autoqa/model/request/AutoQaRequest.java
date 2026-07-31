package com.br.criarcenariotestes.business.autoqa.model.request;

import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationType;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaMode;
import jakarta.validation.constraints.NotBlank;

/**
 * Request principal para iniciar o workflow Auto QA.
 * <p>
 * Regras:
 * - projectPath é obrigatório
 * - Deve haver scenarioId ou scenarioText
 * - allowFileUpdate é false por padrão
 * - executeAfterGeneration é false por padrão
 */
public record AutoQaRequest(

        String title,

        String scenarioId,

        String scenarioText,

        @NotBlank(message = "O caminho do projeto é obrigatório")
        String projectPath,

        AutomationFramework framework,

        AutomationLanguage language,

        AutomationType automationType,

        AutoQaMode mode,

        boolean executeAfterGeneration,

        boolean allowFileUpdate

) {

    public AutoQaRequest {
        if (scenarioId == null && (scenarioText == null || scenarioText.isBlank())) {
            throw new IllegalArgumentException(
                    "É necessário informar scenarioId ou scenarioText"
            );
        }
    }

    public AutoQaMode modeOrDefault() {
        return mode != null ? mode : AutoQaMode.GENERATE_FOR_REVIEW;
    }

    public AutomationType automationTypeOrDefault() {
        return automationType != null ? automationType : AutomationType.WEB;
    }
}
