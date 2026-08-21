package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.*;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.learning.*;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("LearningService - Testes Unitários")
class LearningServiceTest {

    @Test
    @DisplayName("Deve coletar aprendizado positivo para ExecutionStatus.PASSED sem chamar IA (determinístico coerente)")
    void deveColetarAprendizadoPositivoParaPassed() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED), apply(id), execution(id), noFailure(id));

        assertThat(result.positiveLearnings()).isGreaterThan(0);
        assertThat(result.status()).isIn(LearningStatus.COLLECTED, LearningStatus.COLLECTED_WITH_WARNINGS, LearningStatus.REVIEW_REQUIRED);
        verifyNoInteractions(resolver);
    }

    @Test
    @DisplayName("Deve coletar aprendizado negativo para ExecutionStatus.FAILED sem chamar IA")
    void deveColetarAprendizadoNegativoParaFailed() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED), apply(id), failedExecution(id),
                LearningTestData.analyzedResult(id, com.br.criarcenariotestes.business.autoqa.model.failure.FailureCategory.ASSERTION_FAILURE, "teste"));

        assertThat(result.negativeLearnings()).isGreaterThan(0);
        verifyNoInteractions(resolver);
    }

    @Test
    @DisplayName("Status operacional (ExecutionStatus.ERROR) deve gerar BLOCKED sem chamar IA")
    void statusOperacionalGeraBlockedSemIa() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();
        ExecutionResult errorExec = new ExecutionResult(id, null, ExecutionStatus.ERROR, null, Instant.now(), Instant.now(),
                Duration.ofSeconds(1), null, null, false, false, List.of(), List.of(), true);

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED), apply(id), errorExec,
                LearningTestData.blockedResult(id));

        assertThat(result.status()).isEqualTo(LearningStatus.BLOCKED);
        assertThat(result.items()).isEmpty();
        verifyNoInteractions(resolver);
    }

    @Test
    @DisplayName("ApplyStatus.FAILED deve gerar BLOCKED sem chamar IA")
    void applyFailedGeraBlockedSemIa() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();
        var applyFailed = new com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult(id, List.of(), List.of(),
                List.of(), List.of(), "projeto", "backup", com.br.criarcenariotestes.business.autoqa.model.apply.ApplyStatus.FAILED, false, true);

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED), applyFailed, execution(id), noFailure(id));

        assertThat(result.status()).isEqualTo(LearningStatus.BLOCKED);
        verifyNoInteractions(resolver);
    }

    @Test
    @DisplayName("CodeReviewResult INVALID deve gerar BLOCKED sem chamar IA")
    void reviewInvalidoGeraBlockedSemIa() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.INVALID), apply(id), execution(id), noFailure(id));

        assertThat(result.status()).isEqualTo(LearningStatus.BLOCKED);
        verifyNoInteractions(resolver);
    }

    @Test
    @DisplayName("Sem aprendizado determinístico sustentável deve gerar SKIPPED sem chamar IA")
    void semAprendizadoDeterministicoGeraSkippedSemIa() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(),
                knowledgeVazio(), plan(), generationVazia(id), review(ReviewStatus.CHANGES_REQUIRED), apply(id),
                execution(id), noFailure(id));

        assertThat(result.status()).isEqualTo(LearningStatus.SKIPPED);
        assertThat(result.items()).isEmpty();
        verifyNoInteractions(resolver);
    }

    @Test
    @DisplayName("Item determinístico HIGH de scope EXECUTION não deve chamar IA")
    void highDeterministicoExecutionNaoChamaIa() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        TestExecutionSummary s1 = new TestExecutionSummary("PLAYWRIGHT", 3, 3, 0, 0, 0, List.of(), List.of());
        TestExecutionSummary s2 = new TestExecutionSummary("PLAYWRIGHT", 2, 2, 0, 0, 0, List.of(), List.of());
        ExecutionResult twoSummaries = new ExecutionResult(id, LearningTestData.command(), ExecutionStatus.PASSED, 0,
                Instant.now(), Instant.now(), Duration.ofSeconds(3), "ok", "", false, false, List.of(s1, s2), List.of(), true);

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledgeVazio(),
                plan(), generationVazia(id), review(ReviewStatus.APPROVED), applySemArquivos(id), twoSummaries, noFailure(id));

        assertThat(result.items()).anyMatch(i -> i.scope() == LearningScope.EXECUTION && i.confidence() == LearningConfidence.HIGH);
        verifyNoInteractions(resolver);
    }

    @Test
    @DisplayName("Confidence LOW (review com warnings) deve chamar IA")
    void lowConfidenceDeveChamarIa() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        when(resolver.getActiveProvider()).thenReturn(provider);
        when(resolver.getFallbackProvider()).thenReturn(provider);
        when(provider.getName()).thenReturn("primary");
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(emptyAiResponseJson());
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(), plan(), generation(id),
                review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        verify(provider, atLeastOnce()).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("IA não deve remover nem substituir item determinístico com o mesmo id")
    void iaNaoRemoveItemDeterministico() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        when(resolver.getActiveProvider()).thenReturn(provider);
        when(resolver.getFallbackProvider()).thenReturn(provider);
        when(provider.getName()).thenReturn("primary");
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(aiResponseTryingToOverrideJson());
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        assertThat(result.items()).noneMatch(i -> i.source() == LearningSource.AI_SUGGESTION
                && i.title().equalsIgnoreCase("Componente reutilizado com sucesso"));
    }

    @Test
    @DisplayName("IA não deve promover item para scope GLOBAL")
    void iaNaoPromoveGlobal() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        when(resolver.getActiveProvider()).thenReturn(provider);
        when(resolver.getFallbackProvider()).thenReturn(provider);
        when(provider.getName()).thenReturn("primary");
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(aiResponseComGlobalJson());
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        assertThat(result.items()).noneMatch(i -> i.scope() == LearningScope.GLOBAL || i.scope() == LearningScope.TEAM);
    }

    @Test
    @DisplayName("Status/confidence/valid sugeridos pela IA nunca são a fonte da verdade")
    void statusDaIaNuncaEhFonteDaVerdade() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        when(resolver.getActiveProvider()).thenReturn(provider);
        when(resolver.getFallbackProvider()).thenReturn(provider);
        when(provider.getName()).thenReturn("primary");
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(aiResponseComValidFalseJson());
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("Item PROJECT/FRAMEWORK deve nascer PENDING (execução única não vira recorrente)")
    void projectFrameworkNascePending() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED), apply(id), execution(id), noFailure(id));

        assertThat(result.items()).filteredOn(i -> i.scope() == LearningScope.PROJECT || i.scope() == LearningScope.FRAMEWORK)
                .isNotEmpty()
                .allMatch(i -> i.approvalStatus() == LearningApprovalStatus.PENDING);
    }

    @Test
    @DisplayName("Falha técnica em ambos os providers segue com itens determinísticos e avisa")
    void falhaTecnicaEmAmbosProvidersSegueComDeterministicos() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider primary = mock(AiProvider.class);
        AiProvider fallback = mock(AiProvider.class);
        when(resolver.getActiveProvider()).thenReturn(primary);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(primary.getName()).thenReturn("primary");
        when(fallback.getName()).thenReturn("fallback");
        when(primary.gerarResposta(anyString(), anyString())).thenThrow(new RuntimeException("falha técnica"));
        when(fallback.gerarResposta(anyString(), anyString())).thenThrow(new RuntimeException("falha técnica"));
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        // Aprendizado é enriquecimento, não a entrega: derrubar aqui perdia o
        // resultado de nove etapas já concluídas (geração, aplicação, execução)
        // só porque o provider de IA estava fora.
        LearningResult resultado = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        assertThat(resultado).isNotNull();
        assertThat(resultado.warnings()).anyMatch(w -> "AI_LEARNING_UNAVAILABLE".equals(w.code()));
        verify(fallback, times(1)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Não deve chamar fallback quando o provider secundário é o mesmo que o principal")
    void naoDeveChamarFallbackQuandoMesmoProvider() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        when(resolver.getActiveProvider()).thenReturn(provider);
        when(resolver.getFallbackProvider()).thenReturn(provider);
        when(provider.getName()).thenReturn("primary");
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(emptyAiResponseJson());
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(), plan(), generation(id),
                review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        verify(provider, times(1)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Deve rejeitar executionId nulo")
    void deveRejeitarExecutionIdNulo() {
        LearningService service = new LearningService(mock(AiProviderResolver.class));
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.learn(null, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED), apply(id), execution(id), noFailure(id)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar discovery nula")
    void deveRejeitarDiscoveryNula() {
        LearningService service = new LearningService(mock(AiProviderResolver.class));
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.learn(id, null, scenario(), knowledge(), plan(), generation(id),
                review(ReviewStatus.APPROVED), apply(id), execution(id), noFailure(id)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar failureAnalysis nula")
    void deveRejeitarFailureAnalysisNula() {
        LearningService service = new LearningService(mock(AiProviderResolver.class));
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED), apply(id), execution(id), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Resultado nunca deve conter o projectPath (não é recebido pelo service)")
    void resultadoNuncaContemProjectPath() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED), apply(id), execution(id), noFailure(id));

        assertThat(result.items()).allSatisfy(i -> assertThat(i.description()).doesNotContain("/Users/"));
    }

    // --- política de TEAM/GLOBAL sugeridos pela IA ---

    @Test
    @DisplayName("Deve rejeitar TEAM sugerido pela IA sem lançar exceção do learn()")
    void deveRejeitarTeamSugeridoPelaIa() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        stubActiveOnly(resolver, provider);
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(aiResponseComScopeJson("TEAM"));
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        assertThat(result.items()).noneMatch(i -> i.scope() == LearningScope.TEAM);
    }

    @Test
    @DisplayName("Deve rejeitar GLOBAL sugerido pela IA sem lançar exceção do learn()")
    void deveRejeitarGlobalSugeridoPelaIa() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        stubActiveOnly(resolver, provider);
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(aiResponseComScopeJson("GLOBAL"));
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        assertThat(result.items()).noneMatch(i -> i.scope() == LearningScope.GLOBAL);
    }

    @Test
    @DisplayName("Não deve descartar apenas o item TEAM — a resposta inteira é rejeitada, item válido junto não é materializado")
    void deveNaoDescartarTeamSilenciosamente() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        stubActiveOnly(resolver, provider);
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(aiResponseMistaComScopeJson("TEAM"));
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        assertThat(result.items()).noneMatch(i -> i.source() == LearningSource.AI_SUGGESTION);
    }

    @Test
    @DisplayName("Não deve descartar apenas o item GLOBAL — a resposta inteira é rejeitada, item válido junto não é materializado")
    void deveNaoDescartarGlobalSilenciosamente() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        stubActiveOnly(resolver, provider);
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(aiResponseMistaComScopeJson("GLOBAL"));
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        assertThat(result.items()).noneMatch(i -> i.source() == LearningSource.AI_SUGGESTION);
    }

    @Test
    @DisplayName("Não deve usar fallback quando a resposta principal sugerir escopo proibido (falha semântica)")
    void deveNaoUsarFallbackParaScopeProibido() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider primary = mock(AiProvider.class);
        AiProvider fallback = mock(AiProvider.class);
        when(resolver.getActiveProvider()).thenReturn(primary);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(primary.getName()).thenReturn("primary");
        when(fallback.getName()).thenReturn("fallback");
        when(primary.gerarResposta(anyString(), anyString())).thenReturn(aiResponseComScopeJson("GLOBAL"));
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(), plan(), generation(id),
                review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        verify(fallback, never()).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Não deve materializar resposta parcialmente inválida — resultado com escopo proibido deve ter o mesmo total de itens que o cenário puramente determinístico")
    void deveNaoMaterializarRespostaParcialmenteInvalida() {
        // cenário determinístico puro (sem IA), usado como baseline de contagem de itens
        LearningService semIa = new LearningService(mock(AiProviderResolver.class));
        UUID idBaseline = UUID.randomUUID();
        int itensDeterministicos = semIa.learn(idBaseline, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(idBaseline), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(idBaseline),
                execution(idBaseline), noFailure(idBaseline)).items().size();

        // mesmo cenário, mas com IA respondendo um item válido + um item de escopo proibido
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        stubActiveOnly(resolver, provider);
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(aiResponseMistaComScopeJson("TEAM"));
        LearningService comIa = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = comIa.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        assertThat(result.items()).hasSize(itensDeterministicos);
        assertThat(result.items()).noneMatch(i -> i.source() == LearningSource.AI_SUGGESTION);
    }

    @Test
    @DisplayName("TEAM sugerido pela IA deve adicionar warning de resposta rejeitada (UNSUPPORTED_SCOPE)")
    void teamAdicionaWarningDeRespostaRejeitada() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        stubActiveOnly(resolver, provider);
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(aiResponseComScopeJson("TEAM"));
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        assertThat(result.warnings()).anyMatch(w -> w.code().equals("UNSUPPORTED_SCOPE"));
    }

    @Test
    @DisplayName("GLOBAL sugerido pela IA deve adicionar warning de resposta rejeitada (UNSUPPORTED_SCOPE)")
    void globalAdicionaWarningDeRespostaRejeitada() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        stubActiveOnly(resolver, provider);
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(aiResponseComScopeJson("GLOBAL"));
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        assertThat(result.warnings()).anyMatch(w -> w.code().equals("UNSUPPORTED_SCOPE"));
    }

    @Test
    @DisplayName("Nenhum item inválido (escopo proibido) é materializado quando a resposta é rejeitada")
    void nenhumItemInvalidoEMaterializado() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        stubActiveOnly(resolver, provider);
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(aiResponseMistaComScopeJson("GLOBAL"));
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        assertThat(result.items()).noneMatch(i -> i.scope() == LearningScope.GLOBAL || i.scope() == LearningScope.TEAM);
        assertThat(result.items()).noneMatch(i -> i.source() == LearningSource.AI_SUGGESTION);
    }

    @Test
    @DisplayName("Itens determinísticos são preservados quando a resposta da IA é rejeitada por escopo")
    void itensDeterministicosSaoPreservadosQuandoRespostaRejeitada() {
        LearningService semIa = new LearningService(mock(AiProviderResolver.class));
        UUID idBaseline = UUID.randomUUID();
        LearningResult baseline = semIa.learn(idBaseline, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(idBaseline), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(idBaseline),
                execution(idBaseline), noFailure(idBaseline));

        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        stubActiveOnly(resolver, provider);
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(aiResponseComScopeJson("TEAM"));
        LearningService comIa = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = comIa.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        assertThat(result.items()).hasSameSizeAs(baseline.items());
        assertThat(result.positiveLearnings()).isEqualTo(baseline.positiveLearnings());
    }

    @Test
    @DisplayName("Fallback não é chamado quando a resposta é rejeitada por escopo proibido")
    void fallbackNaoEhChamadoQuandoRespostaRejeitada() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider primary = mock(AiProvider.class);
        AiProvider fallback = mock(AiProvider.class);
        when(resolver.getActiveProvider()).thenReturn(primary);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(primary.getName()).thenReturn("primary");
        when(fallback.getName()).thenReturn("fallback");
        when(primary.gerarResposta(anyString(), anyString())).thenReturn(aiResponseComScopeJson("TEAM"));
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(), plan(), generation(id),
                review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        verify(fallback, never()).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Warning de resposta rejeitada não deve conter a resposta bruta da IA, JSON, prompt ou paths")
    void warningNaoContemRespostaBruta() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider provider = mock(AiProvider.class);
        stubActiveOnly(resolver, provider);
        String respostaBruta = aiResponseComScopeJson("TEAM");
        when(provider.gerarResposta(anyString(), anyString())).thenReturn(respostaBruta);
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        LearningResult result = service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        LearningWarning warning = result.warnings().stream().filter(w -> w.code().equals("UNSUPPORTED_SCOPE")).findFirst().orElseThrow();
        assertThat(warning.description())
                .doesNotContain("TEAM")
                .doesNotContain("{")
                .doesNotContain("scope")
                .doesNotContain("/Users/")
                .doesNotContain("Regra sugerida");
    }

    // --- confirmação da política completa de fallback ---

    @Test
    @DisplayName("Parse inválido no principal deve acionar o fallback")
    void parseInvalidoUsaFallback() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider primary = mock(AiProvider.class);
        AiProvider fallback = mock(AiProvider.class);
        when(resolver.getActiveProvider()).thenReturn(primary);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(primary.getName()).thenReturn("primary");
        when(fallback.getName()).thenReturn("fallback");
        when(primary.gerarResposta(anyString(), anyString())).thenReturn("isto não é json");
        when(fallback.gerarResposta(anyString(), anyString())).thenReturn(emptyAiResponseJson());
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(), plan(), generation(id),
                review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        verify(fallback, times(1)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Resposta null do principal deve acionar o fallback")
    void respostaNullUsaFallback() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider primary = mock(AiProvider.class);
        AiProvider fallback = mock(AiProvider.class);
        when(resolver.getActiveProvider()).thenReturn(primary);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(primary.getName()).thenReturn("primary");
        when(fallback.getName()).thenReturn("fallback");
        when(primary.gerarResposta(anyString(), anyString())).thenReturn(null);
        when(fallback.gerarResposta(anyString(), anyString())).thenReturn(emptyAiResponseJson());
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(), plan(), generation(id),
                review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        verify(fallback, times(1)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Resposta em branco do principal deve acionar o fallback")
    void respostaBlankUsaFallback() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider primary = mock(AiProvider.class);
        AiProvider fallback = mock(AiProvider.class);
        when(resolver.getActiveProvider()).thenReturn(primary);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(primary.getName()).thenReturn("primary");
        when(fallback.getName()).thenReturn("fallback");
        when(primary.gerarResposta(anyString(), anyString())).thenReturn("   ");
        when(fallback.gerarResposta(anyString(), anyString())).thenReturn(emptyAiResponseJson());
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(), plan(), generation(id),
                review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        verify(fallback, times(1)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Resposta acima do limite do principal deve acionar o fallback")
    void respostaAcimaDoLimiteUsaFallback() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider primary = mock(AiProvider.class);
        AiProvider fallback = mock(AiProvider.class);
        when(resolver.getActiveProvider()).thenReturn(primary);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(primary.getName()).thenReturn("primary");
        when(fallback.getName()).thenReturn("fallback");
        when(primary.gerarResposta(anyString(), anyString())).thenReturn("a".repeat(60_000));
        when(fallback.gerarResposta(anyString(), anyString())).thenReturn(emptyAiResponseJson());
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(), plan(), generation(id),
                review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        verify(fallback, times(1)).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Erro semântico (escopo proibido) não deve acionar o fallback")
    void erroSemanticoNaoUsaFallback() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider primary = mock(AiProvider.class);
        AiProvider fallback = mock(AiProvider.class);
        when(resolver.getActiveProvider()).thenReturn(primary);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(primary.getName()).thenReturn("primary");
        when(fallback.getName()).thenReturn("fallback");
        when(primary.gerarResposta(anyString(), anyString())).thenReturn(aiResponseComScopeJson("TEAM"));
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(), plan(), generation(id),
                review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        verify(primary, times(1)).gerarResposta(anyString(), anyString());
        verify(fallback, never()).gerarResposta(anyString(), anyString());
    }

    @Test
    @DisplayName("Principal e fallback devem ser chamados no máximo uma vez cada, quando ambos falham tecnicamente")
    void principalEFallbackChamadosNoMaximoUmaVez() {
        AiProviderResolver resolver = mock(AiProviderResolver.class);
        AiProvider primary = mock(AiProvider.class);
        AiProvider fallback = mock(AiProvider.class);
        when(resolver.getActiveProvider()).thenReturn(primary);
        when(resolver.getFallbackProvider()).thenReturn(fallback);
        when(primary.getName()).thenReturn("primary");
        when(fallback.getName()).thenReturn("fallback");
        when(primary.gerarResposta(anyString(), anyString())).thenThrow(new RuntimeException("falha técnica"));
        when(fallback.gerarResposta(anyString(), anyString())).thenThrow(new RuntimeException("falha técnica"));
        LearningService service = new LearningService(resolver);
        UUID id = UUID.randomUUID();

        // O ponto do teste é a CONTAGEM de chamadas (uma por provider), que
        // segue valendo agora que a falha vira aviso em vez de exceção.
        service.learn(id, GenerationTestData.playwrightDiscovery(), scenario(), knowledge(),
                plan(), generation(id), review(ReviewStatus.APPROVED_WITH_WARNINGS), apply(id), execution(id), noFailure(id));

        verify(primary, times(1)).gerarResposta(anyString(), anyString());
        verify(fallback, times(1)).gerarResposta(anyString(), anyString());
    }

    private void stubActiveOnly(AiProviderResolver resolver, AiProvider provider) {
        when(resolver.getActiveProvider()).thenReturn(provider);
        when(resolver.getFallbackProvider()).thenReturn(provider);
        when(provider.getName()).thenReturn("primary");
    }

    private String aiResponseComScopeJson(String scope) {
        return """
                {"items":[{"type":"FRAMEWORK_RULE","scope":"%s","title":"Regra sugerida","description":"descrição","recommendation":"recomendação","relatedComponents":[],"relatedFiles":[],"tags":[]}],"warnings":[],"confidence":"LOW","humanReviewRequired":true,"valid":true}
                """.formatted(scope);
    }

    private String aiResponseMistaComScopeJson(String scope) {
        return """
                {"items":[{"type":"COMMAND_PATTERN","scope":"EXECUTION","title":"Sugestão válida","description":"descrição","recommendation":"recomendação","relatedComponents":[],"relatedFiles":[],"tags":[]},{"type":"FRAMEWORK_RULE","scope":"%s","title":"Regra sugerida","description":"descrição","recommendation":"recomendação","relatedComponents":[],"relatedFiles":[],"tags":[]}],"warnings":[],"confidence":"LOW","humanReviewRequired":true,"valid":true}
                """.formatted(scope);
    }

    // --- helpers de fixture ---

    private ScenarioAnalysisResult scenario() {
        return GenerationTestData.validScenario();
    }

    private ProjectKnowledgeResult knowledge() {
        return LearningTestData.knowledgeWithReuseCandidates("tests/support/loginPage.ts");
    }

    private ProjectKnowledgeResult knowledgeVazio() {
        return LearningTestData.knowledgeWithReuseCandidates();
    }

    private TechnicalPlanResult plan() {
        return GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
    }

    private GenerationResult generation(UUID id) {
        return LearningTestData.generationWithReusedFiles(id, "tests/support/loginPage.ts");
    }

    private GenerationResult generationVazia(UUID id) {
        return LearningTestData.generationWithReusedFiles(id);
    }

    private CodeReviewResult review(ReviewStatus status) {
        return new CodeReviewResult(UUID.randomUUID(), List.of(), List.of(), List.of(), List.of("no-hardcoded-wait"),
                List.of(), List.of(), status, com.br.criarcenariotestes.business.autoqa.model.review.ReviewConfidence.HIGH, false, true);
    }

    private com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult apply(UUID id) {
        return LearningTestData.completedApply(id, "tests/login.spec.ts");
    }

    private com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult applySemArquivos(UUID id) {
        return LearningTestData.completedApply(id);
    }

    private ExecutionResult execution(UUID id) {
        return LearningTestData.passedExecution(id, "PLAYWRIGHT", 3);
    }

    private ExecutionResult failedExecution(UUID id) {
        return LearningTestData.failedExecution(id, "PLAYWRIGHT", 3, 1, "teste");
    }

    private FailureAnalysisResult noFailure(UUID id) {
        return LearningTestData.noFailureResult(id);
    }

    private String emptyAiResponseJson() {
        return """
                {"items":[],"warnings":[],"confidence":"LOW","humanReviewRequired":true,"valid":true}
                """;
    }

    private String aiResponseTryingToOverrideJson() {
        return """
                {"items":[{"type":"REUSABLE_COMPONENT","scope":"PROJECT","title":"Componente reutilizado com sucesso","description":"tentativa de sobrescrever","recommendation":"ignorar","relatedComponents":[],"relatedFiles":["tests/support/loginPage.ts"],"tags":[]}],"warnings":[],"confidence":"LOW","humanReviewRequired":true,"valid":true}
                """;
    }

    private String aiResponseComGlobalJson() {
        return """
                {"items":[{"type":"FRAMEWORK_RULE","scope":"GLOBAL","title":"Regra global sugerida","description":"descrição","recommendation":"recomendação","relatedComponents":[],"relatedFiles":[],"tags":[]}],"warnings":[],"confidence":"LOW","humanReviewRequired":true,"valid":true}
                """;
    }

    private String aiResponseComValidFalseJson() {
        return """
                {"items":[],"warnings":[],"confidence":"HIGH","humanReviewRequired":false,"valid":false}
                """;
    }
}
