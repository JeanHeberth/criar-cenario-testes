package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.agent.ProjectAnalysisAgent;
import com.br.criarcenariotestes.business.autoqa.agent.ProjectDiscoveryAgent;
import com.br.criarcenariotestes.business.autoqa.agent.AutomationPlannerAgent;
import com.br.criarcenariotestes.business.autoqa.framework.AutomationFrameworkAdapter;
import com.br.criarcenariotestes.business.autoqa.framework.AutomationFrameworkResolver;
import com.br.criarcenariotestes.business.autoqa.model.context.AutomationPlan;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectCatalog;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaMode;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.request.AutoQaRequest;
import com.br.criarcenariotestes.business.autoqa.model.response.AutoQaResponse;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import com.br.criarcenariotestes.infrastructure.repository.AutoQaExecutionRepository;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoQaWorkflowService")
class AutoQaWorkflowServiceTest {

    @TempDir
    Path projectDir;

    @Mock ProjectDiscoveryAgent discoveryAgent;
    @Mock ProjectAnalysisAgent analysisAgent;
    @Mock AutomationPlannerAgent plannerAgent;
    @Mock AutomationFrameworkResolver frameworkResolver;
    @Mock AutomationFrameworkAdapter frameworkAdapter;
    @Mock AutoQaExecutionRepository executionRepository;
    @Mock CenarioRepository cenarioRepository;
    @Mock com.br.criarcenariotestes.business.autoqa.agent.CodeGenerationAgent codeGenerationAgent;
    @Mock com.br.criarcenariotestes.business.autoqa.agent.CodeReviewAgent codeReviewAgent;
    @Mock com.br.criarcenariotestes.business.autoqa.agent.TestExecutionAgent testExecutionAgent;
    @Mock com.br.criarcenariotestes.business.autoqa.agent.TestResultAnalysisAgent testResultAnalysisAgent;
    @Mock com.br.criarcenariotestes.business.autoqa.agent.FailureAnalysisAgent failureAnalysisAgent;
    @Mock com.br.criarcenariotestes.business.autoqa.agent.FixSuggestionAgent fixSuggestionAgent;
    @Mock PlaywrightProjectLayoutService playwrightProjectLayoutService;

    private AutoQaWorkflowService service;
    private AutoQaProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AutoQaProperties();
        properties.setEnabled(true);
        service = new AutoQaWorkflowService(
                properties,
                new ProjectPathValidationService(properties),
                discoveryAgent,
                new ProjectScannerService(properties),
                analysisAgent,
                plannerAgent,
                frameworkResolver,
                executionRepository,
                cenarioRepository,
                codeGenerationAgent,
                codeReviewAgent,
                testExecutionAgent,
                testResultAnalysisAgent,
                failureAnalysisAgent,
                fixSuggestionAgent,
                playwrightProjectLayoutService
        );
    }

    private AutoQaRequest requestWithPath(String path) {
        return new AutoQaRequest(
                "Login Test", null, "Fazer login com credenciais válidas",
                path, AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                null, AutoQaMode.GENERATE_FOR_REVIEW, false, false
        );
    }

    private ProjectDiscoveryResult playwrightDiscovery(boolean withDivergence) {
        return ProjectDiscoveryResult.builder()
                .informedFramework(withDivergence ? AutomationFramework.CYPRESS : AutomationFramework.PLAYWRIGHT)
                .detectedFramework(AutomationFramework.PLAYWRIGHT)
                .detectedLanguage(AutomationLanguage.TYPESCRIPT)
                .packageManager(PackageManager.NPM)
                .detectionEvidences(List.of("playwright.config.ts"))
                .divergences(withDivergence ? List.of("Framework diverge") : List.of())
                .warnings(List.of())
                .build();
    }

    private ProjectAnalysisResult emptyAnalysis() {
        return ProjectAnalysisResult.builder()
                .classes(List.of()).pageObjects(List.of())
                .testFiles(List.of()).fixtureFiles(List.of())
                .helperFiles(List.of()).customCommands(List.of())
                .describeBlocks(List.of()).testCases(List.of())
                .conventions(List.of()).gaps(List.of())
                .warnings(List.of()).metadata(Map.of())
                .analyzedAt(LocalDateTime.now()).build();
    }

    private AutomationPlan validPlan() {
        return AutomationPlan.builder()
                .testName("Login test").objective("Verificar login")
                .filesToCreate(List.of("tests/login.spec.ts"))
                .blocked(false).build();
    }

    private AutomationPlan blockedPlan() {
        return AutomationPlan.builder()
                .testName("Blocked").blocked(true)
                .blockedReason("Faltam informações").build();
    }

    // ─── Fluxo feliz ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Fluxo bem-sucedido")
    class HappyPath {

        @Test
        @DisplayName("deve retornar resposta não nula com executionId")
        void returnsResponseWithExecutionId() throws Exception {
            Files.createFile(projectDir.resolve("playwright.config.ts"));
            when(discoveryAgent.discover(any(), any(), any()))
                    .thenReturn(playwrightDiscovery(false));
            when(frameworkResolver.resolve(any())).thenReturn(frameworkAdapter);
            when(frameworkAdapter.ignoredDirectories()).thenReturn(List.of());
            when(analysisAgent.analyze(any(), any())).thenReturn(emptyAnalysis());
            when(plannerAgent.plan(any(), anyString())).thenReturn(validPlan());
            when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AutoQaResponse response = service.analyze(requestWithPath(projectDir.toString()));

            assertThat(response).isNotNull();
            assertThat(response.executionId()).isNotBlank();
        }

        @Test
        @DisplayName("deve retornar status PLAN_READY quando plano não está bloqueado")
        void returnsPlanReadyStatus() throws Exception {
            Files.createFile(projectDir.resolve("playwright.config.ts"));
            when(discoveryAgent.discover(any(), any(), any()))
                    .thenReturn(playwrightDiscovery(false));
            when(frameworkResolver.resolve(any())).thenReturn(frameworkAdapter);
            when(frameworkAdapter.ignoredDirectories()).thenReturn(List.of());
            when(analysisAgent.analyze(any(), any())).thenReturn(emptyAnalysis());
            when(plannerAgent.plan(any(), anyString())).thenReturn(validPlan());
            when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AutoQaResponse response = service.analyze(requestWithPath(projectDir.toString()));

            assertThat(response.status()).isEqualTo(AutoQaStatus.PLAN_READY);
        }

        @Test
        @DisplayName("deve incluir automationPlan na resposta")
        void responseIncludesPlan() throws Exception {
            Files.createFile(projectDir.resolve("playwright.config.ts"));
            when(discoveryAgent.discover(any(), any(), any()))
                    .thenReturn(playwrightDiscovery(false));
            when(frameworkResolver.resolve(any())).thenReturn(frameworkAdapter);
            when(frameworkAdapter.ignoredDirectories()).thenReturn(List.of());
            when(analysisAgent.analyze(any(), any())).thenReturn(emptyAnalysis());
            when(plannerAgent.plan(any(), anyString())).thenReturn(validPlan());
            when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AutoQaResponse response = service.analyze(requestWithPath(projectDir.toString()));

            assertThat(response.automationPlan()).isNotNull();
        }

        @Test
        @DisplayName("deve salvar no MongoDB")
        void savesToMongoDB() throws Exception {
            Files.createFile(projectDir.resolve("playwright.config.ts"));
            when(discoveryAgent.discover(any(), any(), any()))
                    .thenReturn(playwrightDiscovery(false));
            when(frameworkResolver.resolve(any())).thenReturn(frameworkAdapter);
            when(frameworkAdapter.ignoredDirectories()).thenReturn(List.of());
            when(analysisAgent.analyze(any(), any())).thenReturn(emptyAnalysis());
            when(plannerAgent.plan(any(), anyString())).thenReturn(validPlan());
            when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.analyze(requestWithPath(projectDir.toString()));

            verify(executionRepository, atLeastOnce()).save(any());
        }
    }

    // ─── Erros de caminho ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Erros de caminho")
    class PathErrors {

        @Test
        @DisplayName("deve retornar status ERROR quando caminho não existe")
        void returnsErrorForNonExistentPath() {
            AutoQaRequest request = requestWithPath("/caminho/que/nao/existe");
            AutoQaResponse response = service.analyze(request);
            assertThat(response.status()).isEqualTo(AutoQaStatus.ERROR);
            assertThat(response.issues()).isNotEmpty();
        }

        @Test
        @DisplayName("resposta com caminho inválido deve ter executionId")
        void errorResponseHasExecutionId() {
            AutoQaResponse response = service.analyze(requestWithPath("/nao/existe"));
            assertThat(response.executionId()).isNotBlank();
        }
    }

    // ─── Divergência de framework ─────────────────────────────────────────────

    @Test
    @DisplayName("deve retornar ERROR quando framework diverge")
    void returnsErrorOnFrameworkDivergence() throws Exception {
        Files.createFile(projectDir.resolve("playwright.config.ts"));
        when(discoveryAgent.discover(any(), any(), any()))
                .thenReturn(playwrightDiscovery(true)); // divergence=true

        AutoQaResponse response = service.analyze(requestWithPath(projectDir.toString()));

        assertThat(response.status()).isEqualTo(AutoQaStatus.ERROR);
        assertThat(response.issues())
                .anyMatch(i -> i.code().contains("DIVERGENCE") || i.message().toLowerCase().contains("diverge"));
    }

    // ─── Plano bloqueado ──────────────────────────────────────────────────────

    @Test
    @DisplayName("deve retornar ERROR quando plano está bloqueado")
    void returnsErrorOnBlockedPlan() throws Exception {
        Files.createFile(projectDir.resolve("playwright.config.ts"));
        when(discoveryAgent.discover(any(), any(), any()))
                .thenReturn(playwrightDiscovery(false));
        when(frameworkResolver.resolve(any())).thenReturn(frameworkAdapter);
        when(frameworkAdapter.ignoredDirectories()).thenReturn(List.of());
        when(analysisAgent.analyze(any(), any())).thenReturn(emptyAnalysis());
        when(plannerAgent.plan(any(), anyString())).thenReturn(blockedPlan());
        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AutoQaResponse response = service.analyze(requestWithPath(projectDir.toString()));

        assertThat(response.status()).isEqualTo(AutoQaStatus.ERROR);
    }

    // ─── getExecution ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getExecution deve retornar null quando execução não existe")
    void getExecutionReturnsNullWhenNotFound() {
        when(executionRepository.findByExecutionId(anyString()))
                .thenReturn(Optional.empty());
        AutoQaResponse response = service.getExecution("id-inexistente");
        assertThat(response).isNull();
    }
}
