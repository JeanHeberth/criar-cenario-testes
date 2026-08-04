package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.workflow.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.context.FailureAnalysis;
import com.br.criarcenariotestes.business.autoqa.model.context.FixSuggestion;
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

@DisplayName("FixSuggestionAgent")
@ExtendWith(MockitoExtension.class)
class FixSuggestionAgentTest {

    private FixSuggestionAgent agent;

    @Mock
    private AiProviderResolver aiProviderResolver;

    @BeforeEach
    void setUp() {
        agent = new FixSuggestionAgent(aiProviderResolver);
    }

    // ─── Geração de Sugestões ─────────────────────────────────────────────

    @Nested
    @DisplayName("Geração de sugestões de correção")
    class FixSuggestionGeneration {

        @Test
        @DisplayName("gera sugestão para MissingImport")
        void generatesSuggestionForMissingImport() {
            FailureAnalysis failure = new FailureAnalysis(
                    "MissingImport",
                    "Cannot find module 'lodash'",
                    "app.test.ts",
                    5,
                    "Error: Cannot find module 'lodash'",
                    List.of()
            );

            AutoQaContext ctx = new AutoQaContext();
            ctx.addFailureAnalysis(failure);

            agent.execute(ctx);

            assertThat(ctx.getFixSuggestions()).isNotEmpty();
        }

        @Test
        @DisplayName("sugestão contém exemplo de código")
        void suggestionContainsCodeExample() {
            FailureAnalysis failure = new FailureAnalysis(
                    "MissingImport",
                    "Cannot find module 'axios'",
                    "client.ts",
                    3,
                    "Error: Cannot find module 'axios'",
                    List.of()
            );

            AutoQaContext ctx = new AutoQaContext();
            ctx.addFailureAnalysis(failure);

            agent.execute(ctx);

            assertThat(ctx.getFixSuggestions())
                    .anySatisfy(s -> assertThat(s.codeExample())
                            .isNotEmpty());
        }

        @Test
        @DisplayName("sugestão possui prioridade")
        void suggestionHasPriority() {
            FailureAnalysis failure = new FailureAnalysis(
                    "AssertionFailed",
                    "Expected true but got false",
                    "test.ts",
                    42,
                    "AssertionError",
                    List.of()
            );

            AutoQaContext ctx = new AutoQaContext();
            ctx.addFailureAnalysis(failure);

            agent.execute(ctx);

            assertThat(ctx.getFixSuggestions())
                    .anySatisfy(s -> assertThat(s.priority())
                            .isBetween(1, 5));
        }
    }

    // ─── Priorização de Sugestões ─────────────────────────────────────────

    @Nested
    @DisplayName("Priorização de sugestões")
    class SuggestionPrioritization {

        @Test
        @DisplayName("TimeoutError tem prioridade alta")
        void timeoutErrorHasHighPriority() {
            FailureAnalysis failure = new FailureAnalysis(
                    "TimeoutError",
                    "Timeout of 5000ms exceeded",
                    "async.test.ts",
                    20,
                    "Timeout",
                    List.of()
            );

            AutoQaContext ctx = new AutoQaContext();
            ctx.addFailureAnalysis(failure);

            agent.execute(ctx);

            assertThat(ctx.getFixSuggestions())
                    .anySatisfy(s -> assertThat(s.priority()).isGreaterThanOrEqualTo(4));
        }

        @Test
        @DisplayName("MissingImport tem prioridade média")
        void missingImportHasMediumPriority() {
            FailureAnalysis failure = new FailureAnalysis(
                    "MissingImport",
                    "Cannot find module 'xyz'",
                    "app.ts",
                    1,
                    "Error",
                    List.of()
            );

            AutoQaContext ctx = new AutoQaContext();
            ctx.addFailureAnalysis(failure);

            agent.execute(ctx);

            assertThat(ctx.getFixSuggestions())
                    .anySatisfy(s -> assertThat(s.priority())
                            .isBetween(2, 4));
        }
    }

    // ─── Múltiplas Sugestões ──────────────────────────────────────────────

    @Nested
    @DisplayName("Geração de múltiplas sugestões")
    class MultipleSuggestions {

        @Test
        @DisplayName("gera sugestões para cada falha análise")
        void generatesForEachFailure() {
            AutoQaContext ctx = new AutoQaContext();
            ctx.addFailureAnalysis(new FailureAnalysis(
                    "MissingImport",
                    "Cannot find module 'lodash'",
                    "app.ts",
                    1,
                    "Error",
                    List.of()
            ));
            ctx.addFailureAnalysis(new FailureAnalysis(
                    "AssertionFailed",
                    "Expected true but got false",
                    "test.ts",
                    10,
                    "AssertionError",
                    List.of()
            ));

            agent.execute(ctx);

            assertThat(ctx.getFixSuggestions()).hasSize(2);
        }
    }
}
