package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.model.context.AutomationPlan;
import com.br.criarcenariotestes.business.autoqa.model.context.ClassInfo;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaMode;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.request.AutoQaRequest;
import com.br.criarcenariotestes.business.autoqa.prompt.AutoQaPromptFactory;
import com.br.criarcenariotestes.business.autoqa.workflow.AutoQaContext;
import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutomationPlannerAgent")
class AutomationPlannerAgentTest {

    @Mock
    private AiProviderResolver providerResolver;
    @Mock
    private AiProvider aiProvider;

    private AutomationPlannerAgent agent;
    private AutoQaPromptFactory promptFactory;

    @BeforeEach
    void setUp() {
        promptFactory = new AutoQaPromptFactory();
        when(providerResolver.getActiveProvider()).thenReturn(aiProvider);
        agent = new AutomationPlannerAgent(providerResolver, promptFactory);
    }

    private AutoQaContext buildContext(boolean withAnalysis) {
        AutoQaRequest request = new AutoQaRequest(
                "Login Test", null, "Fazer login com credenciais válidas",
                "/tmp/project", AutomationFramework.PLAYWRIGHT,
                AutomationLanguage.TYPESCRIPT, null,
                AutoQaMode.GENERATE_FOR_REVIEW, false, false
        );
        AutoQaContext ctx = new AutoQaContext(request);
        ctx.updateStatus(AutoQaStatus.PLANNING, "TEST");

        ctx.setDiscoveryResult(ProjectDiscoveryResult.builder()
                .detectedFramework(AutomationFramework.PLAYWRIGHT)
                .detectedLanguage(AutomationLanguage.TYPESCRIPT)
                .packageManager(PackageManager.NPM)
                .detectionEvidences(List.of("playwright.config.ts encontrado"))
                .divergences(List.of())
                .warnings(List.of())
                .build());

        if (withAnalysis) {
            ClassInfo loginPage = ClassInfo.builder()
                    .name("LoginPage")
                    .type("class")
                    .methods(List.of())
                    .sourceFile("pages/LoginPage.ts")
                    .build();
            ctx.setProjectAnalysis(ProjectAnalysisResult.builder()
                    .classes(List.of(loginPage))
                    .pageObjects(List.of(loginPage))
                    .testFiles(List.of())
                    .fixtureFiles(List.of())
                    .helperFiles(List.of())
                    .customCommands(List.of())
                    .describeBlocks(List.of())
                    .testCases(List.of())
                    .conventions(List.of())
                    .gaps(List.of())
                    .warnings(List.of())
                    .metadata(Map.of())
                    .build());
        }
        return ctx;
    }

    // ─── Plano válido ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve retornar AutomationPlan não nulo quando AI retorna JSON válido")
    void returnsNonNullPlan() {
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenReturn(validPlanJson());
        AutoQaContext ctx = buildContext(true);
        AutomationPlan plan = agent.plan(ctx, "Fazer login com credenciais válidas");
        assertThat(plan).isNotNull();
    }

    @Test
    @DisplayName("deve extrair testName do JSON da IA")
    void extractsTestName() {
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenReturn(validPlanJson());
        AutomationPlan plan = agent.plan(buildContext(true), "cenário");
        assertThat(plan.getTestName()).isEqualTo("Login com credenciais válidas");
    }

    @Test
    @DisplayName("deve extrair filesToCreate do JSON da IA")
    void extractsFilesToCreate() {
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenReturn(validPlanJson());
        AutomationPlan plan = agent.plan(buildContext(true), "cenário");
        assertThat(plan.getFilesToCreate()).isNotEmpty();
    }

    @Test
    @DisplayName("deve preservar blocked=false quando IA retorna plano válido")
    void planIsNotBlocked() {
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenReturn(validPlanJson());
        AutomationPlan plan = agent.plan(buildContext(true), "cenário");
        assertThat(plan.isBlocked()).isFalse();
    }

    // ─── Plano bloqueado ──────────────────────────────────────────────────────

    @Test
    @DisplayName("deve retornar plano blocked quando IA retorna blocked=true")
    void planIsBlockedWhenAiIndicates() {
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenReturn(blockedPlanJson());
        AutomationPlan plan = agent.plan(buildContext(true), "cenário incompleto");
        assertThat(plan.isBlocked()).isTrue();
        assertThat(plan.getBlockedReason()).isNotBlank();
    }

    // ─── Resposta com markdown ────────────────────────────────────────────────

    @Test
    @DisplayName("deve extrair JSON mesmo quando IA retorna com markdown code fence")
    void handlesMarkdownFence() {
        String withFence = "```json\n" + validPlanJson() + "\n```";
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenReturn(withFence);
        AutomationPlan plan = agent.plan(buildContext(true), "cenário");
        assertThat(plan).isNotNull();
        assertThat(plan.getTestName()).isEqualTo("Login com credenciais válidas");
    }

    // ─── Falha na IA ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve retornar plano bloqueado quando IA retorna JSON inválido")
    void handlesInvalidJson() {
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenReturn("resposta não JSON");
        AutomationPlan plan = agent.plan(buildContext(true), "cenário");
        assertThat(plan.isBlocked()).isTrue();
        assertThat(plan.getBlockedReason()).contains("parse");
    }

    // ─── Chamada à IA ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve chamar AiProvider exatamente uma vez")
    void callsAiProviderOnce() {
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenReturn(validPlanJson());
        agent.plan(buildContext(true), "cenário");
        verify(aiProvider, times(1)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("o prompt deve conter o texto do cenário")
    void promptContainsScenario() {
        when(aiProvider.gerarResposta(anyString(), anyString()))
                .thenReturn(validPlanJson());
        agent.plan(buildContext(true), "Fazer login com usuario_valido e senha_valida");
        verify(aiProvider).gerarResposta(
                anyString(),
                argThat(prompt -> prompt.contains("usuario_valido"))
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String validPlanJson() {
        return """
                {
                  "testName": "Login com credenciais válidas",
                  "objective": "Verificar que o usuário consegue autenticar com dados válidos",
                  "preconditions": ["Usuário cadastrado no sistema"],
                  "requiredData": ["email_valido", "senha_valida"],
                  "existingComponentsToReuse": ["LoginPage"],
                  "existingClassesToUse": ["LoginPage"],
                  "existingMethodsToUse": [],
                  "filesToCreate": ["tests/login/login.spec.ts"],
                  "filesToUpdate": [],
                  "assertions": ["Usuário redirecionado para home"],
                  "risks": [],
                  "pendingItems": [],
                  "missingElements": [],
                  "requiresNewPageObject": false,
                  "requiresUserIntervention": false,
                  "blocked": false,
                  "blockedReason": null
                }
                """;
    }

    private String blockedPlanJson() {
        return """
                {
                  "testName": "Cenário incompleto",
                  "objective": "",
                  "preconditions": [],
                  "requiredData": [],
                  "existingComponentsToReuse": [],
                  "existingClassesToUse": [],
                  "existingMethodsToUse": [],
                  "filesToCreate": [],
                  "filesToUpdate": [],
                  "assertions": [],
                  "risks": [],
                  "pendingItems": [],
                  "missingElements": ["URL da aplicação não definida"],
                  "requiresNewPageObject": false,
                  "requiresUserIntervention": true,
                  "blocked": true,
                  "blockedReason": "Faltam informações essenciais: URL base da aplicação"
                }
                """;
    }
}
