package com.br.criarcenariotestes.business.autoqa.context;

import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.TestingFramework;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AutoQaContext - Testes Unitários")
class AutoQaContextTest {

    @Test
    @DisplayName("Deve criar contexto com status CREATED")
    void deveCriarContextoComStatusCreated() {
        AutoQaContext context = AutoQaContext.create("Cenário válido", "/projeto");

        assertThat(context.getExecutionId()).isNotNull();
        assertThat(context.getScenario()).isEqualTo("Cenário válido");
        assertThat(context.getProjectPath()).isEqualTo("/projeto");
        assertThat(context.getStartedAt()).isNotNull();
        assertThat(context.getFinishedAt()).isNull();
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.CREATED);
        assertThat(context.getCurrentAgent()).isNull();
        assertThat(context.getAgentExecutions()).isEmpty();
        assertThat(context.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("Deve rejeitar cenário nulo")
    void deveRejeitarCenarioNulo() {
        assertThatThrownBy(() -> AutoQaContext.create(null, "/projeto"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scenario");
    }

    @Test
    @DisplayName("Deve rejeitar cenário vazio")
    void deveRejeitarCenarioVazio() {
        assertThatThrownBy(() -> AutoQaContext.create("   ", "/projeto"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scenario");
    }

    @Test
    @DisplayName("Deve rejeitar caminho nulo")
    void deveRejeitarCaminhoNulo() {
        assertThatThrownBy(() -> AutoQaContext.create("Cenário", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectPath");
    }

    @Test
    @DisplayName("Deve rejeitar caminho vazio")
    void deveRejeitarCaminhoVazio() {
        assertThatThrownBy(() -> AutoQaContext.create("Cenário", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectPath");
    }

    @Test
    @DisplayName("Deve remover espaços do cenário e do caminho")
    void deveRemoverEspacosDoCenarioEDoCaminho() {
        AutoQaContext context = AutoQaContext.create("  Cenário  ", "  /projeto  ");

        assertThat(context.getScenario()).isEqualTo("Cenário");
        assertThat(context.getProjectPath()).isEqualTo("/projeto");
    }

    @Test
    @DisplayName("Deve registrar execução do agente")
    void deveRegistrarExecucaoDoAgente() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        AgentExecutionResult result = AgentExecutionResult.success("Executado");

        context.addAgentExecution(result);

        assertThat(context.getAgentExecutions()).containsExactly(result);
    }

    @Test
    @DisplayName("Deve rejeitar resultado nulo")
    void deveRejeitarResultadoNulo() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        assertThatThrownBy(() -> context.addAgentExecution(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve finalizar com sucesso")
    void deveFinalizarComSucesso() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        context.startWorkflow();
        context.finishSuccessfully();

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.FINISHED);
        assertThat(context.getCurrentAgent()).isNull();
        assertThat(context.getFinishedAt()).isNotNull();
        assertThat(context.getFinishedAt()).isAfterOrEqualTo(context.getStartedAt());
    }

    @Test
    @DisplayName("Deve finalizar com erro")
    void deveFinalizarComErro() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        context.startWorkflow();
        context.finishWithError("Falha");

        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.ERROR);
        assertThat(context.getCurrentAgent()).isNull();
        assertThat(context.getErrors()).containsExactly("Falha");
        assertThat(context.getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve retornar listas imutáveis")
    void deveRetornarListasImutaveis() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        assertThatThrownBy(() -> context.getAgentExecutions().add(AgentExecutionResult.success("X")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> context.getErrors().add("erro"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve registrar agente atual")
    void deveRegistrarAgenteAtual() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        context.startAgent("  Agente 1  ");

        assertThat(context.getCurrentAgent()).isEqualTo("Agente 1");
    }

    @Test
    @DisplayName("Deve limpar agente atual ao finalizar")
    void deveLimparAgenteAtualAoFinalizar() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        context.startAgent("Agente 1");
        context.finishSuccessfully();

        assertThat(context.getCurrentAgent()).isNull();
    }

    @Test
    @DisplayName("Deve registrar resultado da descoberta")
    void deveRegistrarResultadoDaDescoberta() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        ProjectDiscoveryResult result = new ProjectDiscoveryResult(
                Path.of("/projeto"),
                AutomationFramework.PLAYWRIGHT,
                AutomationLanguage.TYPESCRIPT,
                PackageManager.NPM,
                BuildTool.NPM,
                Set.of(TestingFramework.PLAYWRIGHT_TEST),
                Set.of(AutomationFramework.PLAYWRIGHT),
                List.of("PLAYWRIGHT"),
                "playwright.config.ts",
                List.of("playwright.config.ts"),
                List.of(),
                DiscoveryConfidence.HIGH,
                true
        );

        context.registerProjectDiscovery(result);

        assertThat(context.getProjectDiscoveryResult()).isEqualTo(result);
    }

    @Test
    @DisplayName("Deve rejeitar resultado da descoberta nulo")
    void deveRejeitarResultadoDaDescobertaNulo() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");

        assertThatThrownBy(() -> context.registerProjectDiscovery(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar segunda descoberta na mesma execução")
    void deveRejeitarSegundaDescobertaNaMesmaExecucao() {
        AutoQaContext context = AutoQaContext.create("Cenário", "/projeto");
        ProjectDiscoveryResult result = new ProjectDiscoveryResult(
                Path.of("/projeto"),
                AutomationFramework.UNKNOWN,
                AutomationLanguage.UNKNOWN,
                PackageManager.UNKNOWN,
                BuildTool.UNKNOWN,
                Set.of(),
                Set.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                DiscoveryConfidence.UNKNOWN,
                true
        );

        context.registerProjectDiscovery(result);

        assertThatThrownBy(() -> context.registerProjectDiscovery(result))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("discovery");
    }
}
