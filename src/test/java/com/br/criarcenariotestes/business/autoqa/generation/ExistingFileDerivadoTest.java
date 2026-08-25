package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.model.generation.*;
import com.br.criarcenariotestes.business.autoqa.model.planning.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExistingFileDerivadoTest {

    private final GenerationValidator validator = new GenerationValidator();

    @Test
    void deveDerivarExistingFileEmVezDeReprovarAGeracao() {
        // Caso real: a IA devolveu existingFile divergente da operação e a
        // geração inteira foi reprovada — descartando uma chamada já paga por
        // um metadado que o sistema calcula sozinho a partir do plano.
        var acao = new PlannedFileAction("playwright.config.ts", FileOperation.UPDATE,
                PlanComponentType.CONFIGURATION, "ajustar baseURL", true, true,
                ApprovalRequirement.NONE, List.of(), List.of());
        // Um TEST acompanha o config: arquivo de configuração é excluído da
        // evidência de framework (regra deliberada), então sozinho ele nunca
        // satisfaria essa validação — e o alvo aqui é outro.
        var acaoTeste = new PlannedFileAction("tests/login.spec.ts", FileOperation.CREATE,
                PlanComponentType.TEST, "teste de login", false, true,
                ApprovalRequirement.NONE, List.of(), List.of());
        var plano = new TechnicalPlanResult("t", "s", List.of(acao, acaoTeste), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), PlanningStatus.READY, PlanningConfidence.HIGH, true);

        UUID id = UUID.randomUUID();
        var arquivo = new GeneratedFile("playwright.config.ts", GeneratedFileOperation.UPDATE,
                PlanComponentType.CONFIGURATION,
                "import { defineConfig } from '@playwright/test';\nexport default defineConfig({});",
                "UTF-8", null,
                GeneratedFileStatus.GENERATED, false, List.of(), List.of(), List.of());
        var teste = new GeneratedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE,
                PlanComponentType.TEST,
                "import { test, expect } from '@playwright/test';\ntest('x', async () => { expect(1).toBe(1); });",
                "UTF-8", null, GeneratedFileStatus.GENERATED, false, List.of(), List.of(), List.of());
        var bruto = new GenerationResult(id, "PLAYWRIGHT", "TYPESCRIPT", List.of(arquivo, teste), List.of(), List.of(),
                null, null, GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);

        var validado = validator.validate(bruto, GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), plano);

        assertThat(validado.files().get(0).existingFile())
                .as("UPDATE implica existingFile=true — derivado, não recusado")
                .isTrue();
    }
}
