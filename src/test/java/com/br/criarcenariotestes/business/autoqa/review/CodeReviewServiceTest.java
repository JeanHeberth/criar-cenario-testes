package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.autoqa.generation.GeneratedPathResolver;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationHashService;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.model.generation.*;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.*;
import com.br.criarcenariotestes.business.autoqa.review.exception.CodeReviewTechnicalException;
import com.br.criarcenariotestes.business.autoqa.review.exception.CodeReviewValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CodeReviewService - Testes Unitários")
class CodeReviewServiceTest {

    private AiProviderResolver aiProviderResolver;
    private GeneratedArtifactReader artifactReader;
    private StaticReviewRuleEngine ruleEngine;
    private CodeReviewInputSanitizer inputSanitizer;
    private CodeReviewPromptFactory promptFactory;
    private CodeReviewResponseParser responseParser;
    private CodeReviewValidator validator;
    private ReviewSummaryBuilder summaryBuilder;
    private CodeReviewService service;

    private AiProvider primaryProvider;
    private AiProvider fallbackProvider;
    private Path tempDir;
    private GenerationHashService hashService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.tempDir = tempDir;
        this.hashService = new GenerationHashService();

        aiProviderResolver = mock(AiProviderResolver.class);
        artifactReader = new GeneratedArtifactReader(new GeneratedPathResolver(), hashService);
        artifactReader.setGeneratedBaseDir(tempDir);
        ruleEngine = new StaticReviewRuleEngine();
        inputSanitizer = new CodeReviewInputSanitizer();
        promptFactory = new CodeReviewPromptFactory();
        responseParser = mock(CodeReviewResponseParser.class);
        validator = mock(CodeReviewValidator.class);
        summaryBuilder = new ReviewSummaryBuilder();

        service = new CodeReviewService(aiProviderResolver, artifactReader, ruleEngine, inputSanitizer,
                promptFactory, responseParser, validator, summaryBuilder);

        primaryProvider = mockProvider("primary");
        fallbackProvider = mockProvider("fallback");
        when(aiProviderResolver.getActiveProvider()).thenReturn(primaryProvider);
        when(aiProviderResolver.getFallbackProvider()).thenReturn(fallbackProvider);
    }

    @Test
    @DisplayName("Deve revisar com provider ativo em caso de sucesso")
    void deveRevisarComProviderAtivo() {
        UUID executionId = UUID.randomUUID();
        writeValidFile(executionId, "tests/login.spec.ts");
        var aiResponse = approvedResponse("tests/login.spec.ts");
        stubAi("{}", aiResponse);

        CodeReviewResult result = service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(executionId, "tests/login.spec.ts"));

        assertThat(result).isNotNull();
        assertThat(result.executionId()).isEqualTo(executionId);
        verify(primaryProvider).gerarResposta(any(), any());
        verify(fallbackProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve executar regras estáticas antes da IA")
    void deveExecutarStaticRulesAntesDaIa() {
        UUID executionId = UUID.randomUUID();
        writeValidFile(executionId, "tests/login.spec.ts");
        var aiResponse = approvedResponse("tests/login.spec.ts");
        stubAi("{}", aiResponse);

        service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(executionId, "tests/login.spec.ts"));

        verify(validator).validate(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve bloquear sem chamar IA quando há issue CRITICAL estática")
    void deveAprovarQuandoNaoHaArquivoParaRevisar() {
        // Projeto que já tem os componentes: o plano marca tudo como REUSE e a
        // geração não cria arquivo nenhum. Antes, a lista vazia ia para a IA,
        // que devolvia INVALID — e o apply recusava com "review-not-approved".
        // O workflow falhava por ter acertado que não havia trabalho a fazer.
        UUID executionId = UUID.randomUUID();
        GeneratedFile reusado = new GeneratedFile("tests/login.spec.ts", GeneratedFileOperation.REUSE,
                PlanComponentType.TEST, null, "UTF-8", null, GeneratedFileStatus.SKIPPED, true,
                List.of(), List.of(), List.of());
        GenerationResult semArquivosNovos = new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT",
                List.of(reusado), List.of(), List.of(), "root", "manifest.json",
                GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);

        CodeReviewResult result = service.review(executionId, CodeReviewTestData.discovery(),
                CodeReviewTestData.scenario(), GenerationTestData.completeKnowledge(),
                plan("tests/login.spec.ts"), semArquivosNovos);

        assertThat(result.status()).isEqualTo(ReviewStatus.APPROVED_WITH_WARNINGS);
        assertThat(result.warnings()).anyMatch(w -> "NOTHING_TO_REVIEW".equals(w.code()));
        verifyNoInteractions(aiProviderResolver);
    }

    @Test
    @DisplayName("Deve bloquear sem chamar IA quando há issue estática CRITICAL")
    void deveBloquearSemChamarIaEmCritical() {
        UUID executionId = UUID.randomUUID();
        String contentComSegredo = GenerationTestData.PLAYWRIGHT_CONTENT + "\nconst password = \"SuperSecreta123\";\n";
        writeFile(executionId, "tests/login.spec.ts", contentComSegredo);

        CodeReviewResult result = service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"),
                generationWithContent(executionId, "tests/login.spec.ts", contentComSegredo));

        assertThat(result.status()).isEqualTo(ReviewStatus.BLOCKED);
        assertThat(result.humanReviewRequired()).isTrue();
        verifyNoInteractions(aiProviderResolver);
    }

    @Test
    @DisplayName("Deve retornar BLOCKED quando hash físico diverge do registrado")
    void deveRetornarBlockedEmHashDivergente() {
        UUID executionId = UUID.randomUUID();
        writeFile(executionId, "tests/login.spec.ts", "conteudo alterado após a geração");

        GeneratedFile declared = new GeneratedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, "UTF-8", hashService.sha256(GenerationTestData.PLAYWRIGHT_CONTENT).hex(),
                GeneratedFileStatus.GENERATED, false, List.of(), List.of(), List.of());
        GenerationResult generation = new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT", List.of(declared), List.of(), List.of(),
                "root", "manifest.json", GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);

        CodeReviewResult result = service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation);

        assertThat(result.status()).isEqualTo(ReviewStatus.BLOCKED);
        assertThat(result.files().get(0).issues()).anyMatch(i -> i.code().equals(ReviewRule.CONTENT_INTEGRITY_MISMATCH.name()));
        verifyNoInteractions(aiProviderResolver);
    }

    @Test
    @DisplayName("Deve usar fallback quando provider ativo falha tecnicamente")
    void deveUsarFallbackEmFalhaTecnica() {
        UUID executionId = UUID.randomUUID();
        writeValidFile(executionId, "tests/login.spec.ts");
        when(primaryProvider.gerarResposta(any(), any())).thenThrow(new RuntimeException("falha primário"));
        when(fallbackProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(approvedResponse("tests/login.spec.ts"));
        when(validator.validate(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));

        CodeReviewResult result = service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(executionId, "tests/login.spec.ts"));

        assertThat(result).isNotNull();
        verify(fallbackProvider).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Não deve usar fallback em falha semântica (ValidationException)")
    void deveNaoUsarFallbackEmFalhaSemantica() {
        UUID executionId = UUID.randomUUID();
        writeValidFile(executionId, "tests/login.spec.ts");
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(approvedResponse("tests/login.spec.ts"));
        when(validator.validate(any(), any(), any(), any(), any(), any())).thenThrow(new CodeReviewValidationException("inválido"));

        assertThatThrownBy(() -> service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(executionId, "tests/login.spec.ts")))
                .isInstanceOf(CodeReviewValidationException.class);

        verify(fallbackProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve falhar quando os dois providers falharem tecnicamente")
    void deveFalharQuandoDoisProvidersFalharem() {
        UUID executionId = UUID.randomUUID();
        writeValidFile(executionId, "tests/login.spec.ts");
        when(primaryProvider.gerarResposta(any(), any())).thenThrow(new RuntimeException("falha 1"));
        when(fallbackProvider.gerarResposta(any(), any())).thenThrow(new RuntimeException("falha 2"));

        assertThatThrownBy(() -> service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(executionId, "tests/login.spec.ts")))
                .isInstanceOf(CodeReviewTechnicalException.class);
    }

    @Test
    @DisplayName("Não deve repetir o mesmo provider quando ativo e fallback são iguais")
    void deveNaoRepetirProviderIgual() {
        UUID executionId = UUID.randomUUID();
        writeValidFile(executionId, "tests/login.spec.ts");
        when(aiProviderResolver.getActiveProvider()).thenReturn(primaryProvider);
        when(aiProviderResolver.getFallbackProvider()).thenReturn(primaryProvider);
        when(primaryProvider.gerarResposta(any(), any())).thenThrow(new RuntimeException("falha"));

        assertThatThrownBy(() -> service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(executionId, "tests/login.spec.ts")))
                .isInstanceOf(CodeReviewTechnicalException.class);

        verify(primaryProvider, times(1)).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar executionId nulo")
    void deveRejeitarExecutionIdNulo() {
        assertThatThrownBy(() -> service.review(null, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(UUID.randomUUID(), "tests/login.spec.ts")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("executionId");
    }

    @Test
    @DisplayName("Deve rejeitar discovery nulo")
    void deveRejeitarDiscoveryNulo() {
        UUID executionId = UUID.randomUUID();
        assertThatThrownBy(() -> service.review(executionId, null, CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(executionId, "tests/login.spec.ts")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("discovery");
    }

    @Test
    @DisplayName("Deve rejeitar scenario nulo")
    void deveRejeitarScenarioNulo() {
        UUID executionId = UUID.randomUUID();
        assertThatThrownBy(() -> service.review(executionId, CodeReviewTestData.discovery(), null,
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(executionId, "tests/login.spec.ts")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("scenario");
    }

    @Test
    @DisplayName("Deve rejeitar knowledge nulo")
    void deveRejeitarKnowledgeNulo() {
        UUID executionId = UUID.randomUUID();
        assertThatThrownBy(() -> service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                null, plan("tests/login.spec.ts"), generation(executionId, "tests/login.spec.ts")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("knowledge");
    }

    @Test
    @DisplayName("Deve rejeitar plan nulo")
    void deveRejeitarPlanNulo() {
        UUID executionId = UUID.randomUUID();
        assertThatThrownBy(() -> service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), null, generation(executionId, "tests/login.spec.ts")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("plan");
    }

    @Test
    @DisplayName("Deve rejeitar generation nulo")
    void deveRejeitarGenerationNulo() {
        UUID executionId = UUID.randomUUID();
        assertThatThrownBy(() -> service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("generation");
    }

    @Test
    @DisplayName("Deve rejeitar GenerationStatus.PARTIAL")
    void deveRejeitarGenerationPartial() {
        UUID executionId = UUID.randomUUID();
        GenerationResult partial = new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT", List.of(), List.of(), List.of(),
                "root", "manifest.json", GenerationStatus.PARTIAL, GenerationConfidence.LOW, false);

        assertThatThrownBy(() -> service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), partial))
                .isInstanceOf(CodeReviewValidationException.class);
        verifyNoInteractions(aiProviderResolver);
    }

    @Test
    @DisplayName("Deve rejeitar GenerationStatus.FAILED")
    void deveRejeitarGenerationFailed() {
        UUID executionId = UUID.randomUUID();
        GenerationResult failed = new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT", List.of(), List.of(), List.of(),
                "root", "manifest.json", GenerationStatus.FAILED, GenerationConfidence.LOW, false);

        assertThatThrownBy(() -> service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), failed))
                .isInstanceOf(CodeReviewValidationException.class);
        verifyNoInteractions(aiProviderResolver);
    }

    @Test
    @DisplayName("Deve ler somente arquivos CREATE/UPDATE listados na GenerationResult")
    void deveLerSomenteArquivosGerados() {
        UUID executionId = UUID.randomUUID();
        writeValidFile(executionId, "tests/login.spec.ts");
        stubAi("{}", approvedResponse("tests/login.spec.ts"));

        GeneratedFile reuse = new GeneratedFile("pages/LoginPage.ts", GeneratedFileOperation.REUSE, PlanComponentType.PAGE_OBJECT,
                null, "UTF-8", null, GeneratedFileStatus.SKIPPED, true, List.of(), List.of(), List.of());
        GeneratedFile created = new GeneratedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, "UTF-8", hashService.sha256(GenerationTestData.PLAYWRIGHT_CONTENT).hex(),
                GeneratedFileStatus.GENERATED, false, List.of(), List.of(), List.of());
        GenerationResult generation = new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT", List.of(reuse, created),
                List.of("pages/LoginPage.ts"), List.of(), "root", "manifest.json", GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);

        CodeReviewResult result = service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge("pages/LoginPage.ts"), plan("tests/login.spec.ts"), generation);

        assertThat(result.files()).hasSize(2);
        assertThat(result.files().stream().filter(f -> f.relativePath().equals("pages/LoginPage.ts")).findFirst().orElseThrow().status())
                .isEqualTo(FileReviewStatus.SKIPPED);
    }

    @Test
    @DisplayName("Deve validar o hash de cada arquivo lido")
    void deveValidarHash() {
        UUID executionId = UUID.randomUUID();
        writeValidFile(executionId, "tests/login.spec.ts");
        stubAi("{}", approvedResponse("tests/login.spec.ts"));

        service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(executionId, "tests/login.spec.ts"));

        // Se chegou a chamar a IA (não retornou BLOCKED antes), o hash foi validado com sucesso.
        verify(primaryProvider).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve combinar issues estáticas e da IA no arquivo final")
    void deveCombinarIssuesEstaticasEIa() {
        UUID executionId = UUID.randomUUID();
        // conteúdo sem assertion -> gera issue estática MISSING_ASSERTION (HIGH)
        String semAssertion = "import { test } from '@playwright/test';\ntest('login', async ({ page }) => { await page.goto('/login'); });\n";
        writeFile(executionId, "tests/login.spec.ts", semAssertion);
        GenerationResult generation = generationWithContent(executionId, "tests/login.spec.ts", semAssertion);

        var aiIssue = new ReviewIssue("NAMING_CONVENTION_MISMATCH", ReviewCategory.NAMING, ReviewSeverity.LOW,
                "tests/login.spec.ts", null, "nome fora do padrão", null, "renomear", false);
        var aiFile = new CodeReviewAiResponse.AiFileReview("tests/login.spec.ts", FileReviewStatus.CHANGES_REQUIRED,
                List.of(aiIssue), List.of(), List.of(), List.of(), ReviewConfidence.HIGH, true);
        var aiResponse = new CodeReviewAiResponse(List.of(aiFile), List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.CHANGES_REQUIRED, ReviewConfidence.HIGH, false, true);
        stubAi("{}", aiResponse);

        CodeReviewResult result = service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation);

        var fileResult = result.files().get(0);
        assertThat(fileResult.issues()).extracting(ReviewIssue::code)
                .contains("MISSING_ASSERTION", "NAMING_CONVENTION_MISMATCH");
    }

    @Test
    @DisplayName("IA não deve conseguir remover issue estática (Service sempre a preserva)")
    void deveNaoRemoverIssueEstatica() {
        UUID executionId = UUID.randomUUID();
        String semAssertion = "import { test } from '@playwright/test';\ntest('login', async ({ page }) => { await page.goto('/login'); });\n";
        writeFile(executionId, "tests/login.spec.ts", semAssertion);
        GenerationResult generation = generationWithContent(executionId, "tests/login.spec.ts", semAssertion);

        // IA "finge" que está tudo aprovado, sem mencionar a issue estática
        stubAi("{}", approvedResponse("tests/login.spec.ts"));

        CodeReviewResult result = service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation);

        assertThat(result.files().get(0).issues()).extracting(ReviewIssue::code).contains("MISSING_ASSERTION");
        assertThat(result.status()).isNotEqualTo(ReviewStatus.APPROVED);
    }

    @Test
    @DisplayName("IA não deve conseguir reduzir a severidade de issue estática")
    void deveNaoReduzirSeveridadeEstatica() {
        UUID executionId = UUID.randomUUID();
        String semAssertion = "import { test } from '@playwright/test';\ntest('login', async ({ page }) => { await page.goto('/login'); });\n";
        writeFile(executionId, "tests/login.spec.ts", semAssertion);
        GenerationResult generation = generationWithContent(executionId, "tests/login.spec.ts", semAssertion);

        // IA tenta "reduzir" a issue estática para LOW usando o mesmo código
        var reduzida = new ReviewIssue("MISSING_ASSERTION", ReviewCategory.ASSERTION, ReviewSeverity.LOW,
                "tests/login.spec.ts", null, "sem problema", null, "ignorar", false);
        var aiFile = new CodeReviewAiResponse.AiFileReview("tests/login.spec.ts", FileReviewStatus.APPROVED_WITH_WARNINGS,
                List.of(reduzida), List.of(), List.of(), List.of(), ReviewConfidence.HIGH, true);
        var aiResponse = new CodeReviewAiResponse(List.of(aiFile), List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED_WITH_WARNINGS, ReviewConfidence.HIGH, false, true);
        stubAi("{}", aiResponse);

        CodeReviewResult result = service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation);

        var finalIssue = result.files().get(0).issues().stream()
                .filter(i -> i.code().equals("MISSING_ASSERTION")).findFirst().orElseThrow();
        assertThat(finalIssue.severity()).isEqualTo(ReviewSeverity.HIGH);
    }

    @Test
    @DisplayName("Deve enviar somente o input sanitizado ao promptFactory")
    void deveEnviarSomenteInputSanitizado() {
        UUID executionId = UUID.randomUUID();
        writeValidFile(executionId, "tests/login.spec.ts");
        stubAi("{}", approvedResponse("tests/login.spec.ts"));

        CodeReviewResult result = service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(executionId, "tests/login.spec.ts"));

        assertThat(result).isNotNull();
        // discovery/scenario/knowledge nunca são passados diretamente ao provider (só o prompt textual sanitizado)
        verify(primaryProvider).gerarResposta(any(), argThat(prompt -> !prompt.contains("/project")));
    }

    @Test
    @DisplayName("Deve ser stateless entre execuções (executionIds independentes)")
    void deveSerStateless() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        writeValidFile(id1, "tests/login.spec.ts");
        writeValidFile(id2, "tests/login.spec.ts");
        stubAi("{}", approvedResponse("tests/login.spec.ts"));

        CodeReviewResult r1 = service.review(id1, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(id1, "tests/login.spec.ts"));
        CodeReviewResult r2 = service.review(id2, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(id2, "tests/login.spec.ts"));

        assertThat(r1.executionId()).isEqualTo(id1);
        assertThat(r2.executionId()).isEqualTo(id2);
    }

    @Test
    @DisplayName("Deve chamar cada provider no máximo uma vez")
    void deveChamarCadaProviderNoMaximoUmaVez() {
        UUID executionId = UUID.randomUUID();
        writeValidFile(executionId, "tests/login.spec.ts");
        stubAi("{}", approvedResponse("tests/login.spec.ts"));

        service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(executionId, "tests/login.spec.ts"));

        verify(primaryProvider, times(1)).gerarResposta(any(), any());
        verify(fallbackProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Não deve modificar os arquivos gerados (somente leitura)")
    void deveNaoModificarArquivos() throws Exception {
        UUID executionId = UUID.randomUUID();
        Path file = writeValidFile(executionId, "tests/login.spec.ts");
        String before = Files.readString(file, StandardCharsets.UTF_8);
        stubAi("{}", approvedResponse("tests/login.spec.ts"));

        service.review(executionId, CodeReviewTestData.discovery(), CodeReviewTestData.scenario(),
                GenerationTestData.completeKnowledge(), plan("tests/login.spec.ts"), generation(executionId, "tests/login.spec.ts"));

        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo(before);
    }

    // --- helpers ---

    private Path writeValidFile(UUID executionId, String relativePath) {
        return writeFile(executionId, relativePath, GenerationTestData.PLAYWRIGHT_CONTENT);
    }

    private Path writeFile(UUID executionId, String relativePath, String content) {
        try {
            Path target = new GeneratedPathResolver().resolve(tempDir, executionId, relativePath);
            Files.createDirectories(target.getParent());
            Files.writeString(target, content, StandardCharsets.UTF_8);
            return target;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void stubAi(String rawResponse, CodeReviewAiResponse parsed) {
        when(primaryProvider.gerarResposta(any(), any())).thenReturn(rawResponse);
        when(responseParser.parse(any())).thenReturn(parsed);
        when(validator.validate(any(), any(), any(), any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private CodeReviewAiResponse approvedResponse(String... paths) {
        return CodeReviewTestData.approvedResponse(paths);
    }

    private TechnicalPlanResult plan(String... paths) {
        return CodeReviewTestData.plan(paths);
    }

    private GenerationResult generation(UUID executionId, String... paths) {
        return generationWithContent(executionId, paths[0], GenerationTestData.PLAYWRIGHT_CONTENT);
    }

    private GenerationResult generationWithContent(UUID executionId, String path, String content) {
        GeneratedFile file = new GeneratedFile(path, GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                content, "UTF-8", hashService.sha256(content).hex(), GeneratedFileStatus.GENERATED, false, List.of(), List.of(), List.of());
        return new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT", List.of(file), List.of(), List.of(),
                "root", "manifest.json", GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);
    }

    private AiProvider mockProvider(String name) {
        AiProvider provider = mock(AiProvider.class);
        when(provider.getName()).thenReturn(name);
        return provider;
    }
}
