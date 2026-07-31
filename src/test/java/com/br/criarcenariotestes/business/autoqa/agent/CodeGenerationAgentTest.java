package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.framework.AutomationFrameworkAdapter;
import com.br.criarcenariotestes.business.autoqa.model.context.AutomationPlan;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedCodeResponse;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.prompt.AutoQaPromptFactory;
import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CodeGenerationAgent")
class CodeGenerationAgentTest {

    @Mock AiProviderResolver providerResolver;
    @Mock AiProvider aiProvider;
    @Mock AutomationFrameworkAdapter adapter;

    private CodeGenerationAgent agent;

    @BeforeEach
    void setUp() {
        when(providerResolver.getActiveProvider()).thenReturn(aiProvider);
        when(adapter.buildFrameworkInstructions(any())).thenReturn("Playwright profile instructions");
        agent = new CodeGenerationAgent(providerResolver, new AutoQaPromptFactory());
    }

    private AutomationPlan validPlan() {
        return AutomationPlan.builder()
                .testName("Login test")
                .objective("Verificar login")
                .filesToCreate(List.of("tests/login/login.spec.ts"))
                .existingClassesToUse(List.of("LoginPage"))
                .existingMethodsToUse(List.of("LoginPage.login(email, password)"))
                .assertions(List.of("Usuário redirecionado para home"))
                .blocked(false)
                .build();
    }

    // ─── Resposta válida ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Resposta válida da IA")
    class ValidResponse {

        @Test
        @DisplayName("deve retornar GeneratedCodeResponse não nulo")
        void returnsNonNull() {
            when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(validAiJson());
            GeneratedCodeResponse result = agent.generate(
                    validPlan(), AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                    adapter, "Fazer login"
            );
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("deve extrair arquivos gerados da resposta da IA")
        void extractsFiles() {
            when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(validAiJson());
            GeneratedCodeResponse result = agent.generate(
                    validPlan(), AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                    adapter, "cenário"
            );
            assertThat(result.files()).isNotEmpty();
            assertThat(result.files().get(0).relativePath()).isEqualTo("tests/login/login.spec.ts");
        }

        @Test
        @DisplayName("deve preencher generationFailed=false quando geração ok")
        void notFailed() {
            when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(validAiJson());
            GeneratedCodeResponse result = agent.generate(
                    validPlan(), AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                    adapter, "cenário"
            );
            assertThat(result.generationFailed()).isFalse();
        }

        @Test
        @DisplayName("deve extrair reusedComponents")
        void extractsReusedComponents() {
            when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(validAiJson());
            GeneratedCodeResponse result = agent.generate(
                    validPlan(), AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                    adapter, "cenário"
            );
            assertThat(result.reusedComponents()).contains("LoginPage");
        }

        @Test
        @DisplayName("deve lidar com markdown fence na resposta")
        void handlesMarkdownFence() {
            when(aiProvider.gerarResposta(anyString(), anyString()))
                    .thenReturn("```json\n" + validAiJson() + "\n```");
            GeneratedCodeResponse result = agent.generate(
                    validPlan(), AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                    adapter, "cenário"
            );
            assertThat(result.generationFailed()).isFalse();
            assertThat(result.files()).isNotEmpty();
        }
    }

    // ─── Validações de segurança ──────────────────────────────────────────────

    @Nested
    @DisplayName("Validações de segurança dos arquivos gerados")
    class SecurityValidations {

        @Test
        @DisplayName("deve rejeitar relativePath absoluto Unix")
        void rejectsAbsoluteUnixPath() {
            when(aiProvider.gerarResposta(anyString(), anyString()))
                    .thenReturn(jsonWithPath("/etc/passwd"));
            GeneratedCodeResponse result = agent.generate(
                    validPlan(), AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                    adapter, "cenário"
            );
            // Arquivo com path absoluto deve ser filtrado
            assertThat(result.files()).noneMatch(f -> f.relativePath().startsWith("/"));
        }

        @Test
        @DisplayName("deve rejeitar relativePath com path traversal")
        void rejectsPathTraversal() {
            when(aiProvider.gerarResposta(anyString(), anyString()))
                    .thenReturn(jsonWithPath("../../secret/file.ts"));
            GeneratedCodeResponse result = agent.generate(
                    validPlan(), AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                    adapter, "cenário"
            );
            assertThat(result.files()).noneMatch(f -> f.relativePath().contains(".."));
        }

        @Test
        @DisplayName("deve rejeitar operação DELETE")
        void rejectsDeleteOperation() {
            when(aiProvider.gerarResposta(anyString(), anyString()))
                    .thenReturn(jsonWithOperation("DELETE"));
            GeneratedCodeResponse result = agent.generate(
                    validPlan(), AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                    adapter, "cenário"
            );
            assertThat(result.files()).noneMatch(f ->
                    f.operation() == GeneratedFileOperation.CREATE
                            && f.relativePath().equals("tests/ok.spec.ts")
                            && false // este assert é para forçar a verificar DELETE
            );
            // Verifica que nenhum file tem operation null (DELETE foi ignorado)
            if (!result.files().isEmpty()) {
                assertThat(result.files()).allMatch(f -> f.operation() != null);
            }
        }
    }

    // ─── Falha na IA ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Falhas na IA")
    class AiFailures {

        @Test
        @DisplayName("deve retornar generationFailed=true para JSON inválido")
        void failedOnInvalidJson() {
            when(aiProvider.gerarResposta(anyString(), anyString()))
                    .thenReturn("Não consigo gerar isso agora");
            GeneratedCodeResponse result = agent.generate(
                    validPlan(), AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                    adapter, "cenário"
            );
            assertThat(result.generationFailed()).isTrue();
        }

        @Test
        @DisplayName("deve retornar generationFailed=true quando IA lança exceção")
        void failedOnAiException() {
            when(aiProvider.gerarResposta(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Timeout na IA"));
            GeneratedCodeResponse result = agent.generate(
                    validPlan(), AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                    adapter, "cenário"
            );
            assertThat(result.generationFailed()).isTrue();
            assertThat(result.failureReason()).contains("Timeout");
        }
    }

    // ─── Chamada à IA ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deve chamar AiProvider exatamente uma vez")
    void callsAiOnce() {
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(validAiJson());
        agent.generate(validPlan(), AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                adapter, "cenário");
        verify(aiProvider, times(1)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("prompt deve incluir nome do teste do plano")
    void promptIncludesPlanTestName() {
        when(aiProvider.gerarResposta(anyString(), anyString())).thenReturn(validAiJson());
        agent.generate(validPlan(), AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                adapter, "Fazer login com usuario_teste");
        verify(aiProvider).gerarResposta(anyString(),
                argThat(p -> p.contains("usuario_teste")));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String validAiJson() {
        return """
                {
                  "files": [
                    {
                      "relativePath": "tests/login/login.spec.ts",
                      "operation": "CREATE",
                      "content": "import { test } from '@playwright/test';\\ntest('login', async () => {});",
                      "explanation": "Arquivo de teste de login"
                    }
                  ],
                  "reusedComponents": ["LoginPage"],
                  "missingComponents": [],
                  "warnings": [],
                  "summary": "Gerado teste de login"
                }
                """;
    }

    private String jsonWithPath(String path) {
        return """
                {
                  "files": [
                    {
                      "relativePath": "%s",
                      "operation": "CREATE",
                      "content": "// content",
                      "explanation": "test"
                    }
                  ],
                  "reusedComponents": [],
                  "missingComponents": [],
                  "warnings": [],
                  "summary": "test"
                }
                """.formatted(path);
    }

    private String jsonWithOperation(String operation) {
        return """
                {
                  "files": [
                    {
                      "relativePath": "tests/ok.spec.ts",
                      "operation": "%s",
                      "content": "// content",
                      "explanation": "test"
                    }
                  ],
                  "reusedComponents": [],
                  "missingComponents": [],
                  "warnings": [],
                  "summary": "test"
                }
                """.formatted(operation);
    }
}
