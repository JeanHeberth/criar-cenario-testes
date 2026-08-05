package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationTechnicalException;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationValidationException;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationWriteException;
import com.br.criarcenariotestes.business.autoqa.model.generation.*;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("GenerationService - Testes Unitários")
class GenerationServiceTest {

    private AiProviderResolver aiProviderResolver;
    private GenerationInputSanitizer inputSanitizer;
    private GenerationPromptFactory promptFactory;
    private GenerationResponseParser responseParser;
    private GenerationValidator validator;
    private GeneratedFileWriter fileWriter;
    private GenerationHashService hashService;
    private GenerationManifestWriter manifestWriter;
    private GenerationService service;

    private AiProvider primaryProvider;
    private AiProvider fallbackProvider;
    private Path tempDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        this.tempDir = tempDir;
        aiProviderResolver = mock(AiProviderResolver.class);
        inputSanitizer = mock(GenerationInputSanitizer.class);
        promptFactory = mock(GenerationPromptFactory.class);
        responseParser = mock(GenerationResponseParser.class);
        validator = mock(GenerationValidator.class);
        fileWriter = Mockito.spy(new GeneratedFileWriter(new GeneratedPathResolver()));
        hashService = Mockito.spy(new GenerationHashService());
        manifestWriter = Mockito.spy(new GenerationManifestWriter(new ObjectMapper()));

        service = new GenerationService(aiProviderResolver, inputSanitizer, promptFactory, responseParser, validator,
                fileWriter, hashService, manifestWriter);
        service.setGeneratedBaseDir(tempDir);

        primaryProvider = mockProvider("primary");
        fallbackProvider = mockProvider("fallback");
        when(aiProviderResolver.getActiveProvider()).thenReturn(primaryProvider);
        when(aiProviderResolver.getFallbackProvider()).thenReturn(fallbackProvider);
        when(inputSanitizer.sanitize(any(), any(), any(), any())).thenReturn(mock(SanitizedGenerationInput.class));
        when(promptFactory.createSystemPrompt()).thenReturn("system");
        when(promptFactory.createUserPrompt(any())).thenReturn("user");
    }

    @Test
    @DisplayName("Deve gerar com provider ativo em caso de sucesso")
    void deveGerarComProviderAtivo() {
        var aiResult = rawResultOneFile();
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(aiResult);
        when(validator.validate(any(), any(), any(), any(), any())).thenReturn(aiResult);

        GenerationResult result = service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan());

        assertThat(result).isNotNull();
        verify(primaryProvider).gerarResposta(any(), any());
        verify(fallbackProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve usar fallback quando provider ativo falha tecnicamente")
    void deveUsarFallbackEmFalhaTecnica() {
        var aiResult = rawResultOneFile();
        when(primaryProvider.gerarResposta(any(), any())).thenThrow(new RuntimeException("falha primário"));
        when(fallbackProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(aiResult);
        when(validator.validate(any(), any(), any(), any(), any())).thenReturn(aiResult);

        GenerationResult result = service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan());

        assertThat(result).isNotNull();
        verify(fallbackProvider).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Não deve usar fallback em falha semântica (ValidationException)")
    void deveNaoUsarFallbackEmFalhaSemantica() {
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(rawResultOneFile());
        when(validator.validate(any(), any(), any(), any(), any())).thenThrow(new GenerationValidationException("inválido"));

        assertThatThrownBy(() -> service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan()))
                .isInstanceOf(GenerationValidationException.class);

        verify(fallbackProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve falhar quando os dois providers falharem tecnicamente")
    void deveFalharQuandoDoisProvidersFalharem() {
        when(primaryProvider.gerarResposta(any(), any())).thenThrow(new RuntimeException("falha 1"));
        when(fallbackProvider.gerarResposta(any(), any())).thenThrow(new RuntimeException("falha 2"));

        assertThatThrownBy(() -> service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan()))
                .isInstanceOf(GenerationTechnicalException.class);
    }

    @Test
    @DisplayName("Não deve repetir o mesmo provider quando ativo e fallback são iguais")
    void deveNaoRepetirProviderIgual() {
        when(aiProviderResolver.getActiveProvider()).thenReturn(primaryProvider);
        when(aiProviderResolver.getFallbackProvider()).thenReturn(primaryProvider);
        when(primaryProvider.gerarResposta(any(), any())).thenThrow(new RuntimeException("falha"));

        assertThatThrownBy(() -> service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan()))
                .isInstanceOf(GenerationTechnicalException.class);

        verify(primaryProvider, times(1)).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar executionId nulo")
    void deveRejeitarExecutionIdNulo() {
        assertThatThrownBy(() -> service.generate(null, GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("executionId");
    }

    @Test
    @DisplayName("Deve rejeitar discovery nulo")
    void deveRejeitarDiscoveryNulo() {
        assertThatThrownBy(() -> service.generate(UUID.randomUUID(), null,
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("discovery");
    }

    @Test
    @DisplayName("Deve rejeitar scenario nulo")
    void deveRejeitarScenarioNulo() {
        assertThatThrownBy(() -> service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                null, GenerationTestData.completeKnowledge(), onePlan()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scenario");
    }

    @Test
    @DisplayName("Deve rejeitar knowledge nulo")
    void deveRejeitarKnowledgeNulo() {
        assertThatThrownBy(() -> service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), null, onePlan()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("knowledge");
    }

    @Test
    @DisplayName("Deve rejeitar plan nulo")
    void deveRejeitarPlanNulo() {
        assertThatThrownBy(() -> service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plan");
    }

    @Test
    @DisplayName("Deve rejeitar scenario INVALID sem chamar provider")
    void deveRejeitarScenarioInvalid() {
        assertThatThrownBy(() -> service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.invalidScenario(), GenerationTestData.completeKnowledge(), onePlan()))
                .isInstanceOf(GenerationValidationException.class);

        verify(primaryProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar knowledge FAILED sem chamar provider")
    void deveRejeitarKnowledgeFailed() {
        assertThatThrownBy(() -> service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.failedKnowledge(), onePlan()))
                .isInstanceOf(GenerationValidationException.class);

        verify(primaryProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar plan BLOCKED sem chamar provider")
    void deveRejeitarPlanBlocked() {
        assertThatThrownBy(() -> service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), GenerationTestData.blockedPlan()))
                .isInstanceOf(GenerationValidationException.class);

        verify(primaryProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar plan INVALID sem chamar provider")
    void deveRejeitarPlanInvalid() {
        assertThatThrownBy(() -> service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), GenerationTestData.invalidPlan()))
                .isInstanceOf(GenerationValidationException.class);

        verify(primaryProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve rejeitar framework UNKNOWN sem chamar provider")
    void deveRejeitarFrameworkUnknown() {
        assertThatThrownBy(() -> service.generate(UUID.randomUUID(), GenerationTestData.unknownDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan()))
                .isInstanceOf(GenerationValidationException.class);

        verify(primaryProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve validar antes de escrever (nenhuma escrita se validator falhar)")
    void deveValidarAntesDeEscrever() {
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(rawResultOneFile());
        when(validator.validate(any(), any(), any(), any(), any())).thenThrow(new GenerationValidationException("inválido"));

        assertThatThrownBy(() -> service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan()))
                .isInstanceOf(GenerationValidationException.class);

        verify(fileWriter, never()).write(any(), any(), any());
        verify(manifestWriter, never()).write(any(), any(), any());
    }

    @Test
    @DisplayName("Não deve criar diretório de execução em falha semântica")
    void deveNaoEscreverEmFalhaSemantica() {
        UUID executionId = UUID.randomUUID();
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(rawResultOneFile());
        when(validator.validate(any(), any(), any(), any(), any())).thenThrow(new GenerationValidationException("inválido"));

        assertThatThrownBy(() -> service.generate(executionId, GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan()))
                .isInstanceOf(GenerationValidationException.class);

        assertThat(Files.exists(tempDir.resolve(executionId.toString()))).isFalse();
    }

    @Test
    @DisplayName("Deve escrever arquivos após validação")
    void deveEscreverArquivosAposValidacao() {
        UUID executionId = UUID.randomUUID();
        var aiResult = rawResultOneFile();
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(aiResult);
        when(validator.validate(any(), any(), any(), any(), any())).thenReturn(aiResult);

        GenerationResult result = service.generate(executionId, GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan());

        Path expectedFile = tempDir.resolve(executionId.toString()).resolve("files").resolve("tests/login.spec.ts");
        assertThat(Files.exists(expectedFile)).isTrue();
        assertThat(result.files()).anyMatch(f -> f.status() == GeneratedFileStatus.GENERATED);
    }

    @Test
    @DisplayName("Deve calcular hashes dos arquivos gerados")
    void deveCalcularHashes() {
        UUID executionId = UUID.randomUUID();
        var aiResult = rawResultOneFile();
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(aiResult);
        when(validator.validate(any(), any(), any(), any(), any())).thenReturn(aiResult);

        GenerationResult result = service.generate(executionId, GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan());

        GeneratedFile generated = result.files().stream().filter(f -> f.status() == GeneratedFileStatus.GENERATED).findFirst().orElseThrow();
        assertThat(generated.sha256()).isNotBlank();
        assertThat(generated.sha256()).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("Deve escrever manifest.json ao final")
    void deveEscreverManifest() {
        UUID executionId = UUID.randomUUID();
        var aiResult = rawResultOneFile();
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(aiResult);
        when(validator.validate(any(), any(), any(), any(), any())).thenReturn(aiResult);

        GenerationResult result = service.generate(executionId, GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan());

        Path manifest = tempDir.resolve(executionId.toString()).resolve("manifest.json");
        assertThat(Files.exists(manifest)).isTrue();
        assertThat(result.manifestRelativePath()).isEqualTo(executionId + "/manifest.json");
    }

    @Test
    @DisplayName("Deve limpar arquivos parciais em falha de escrita do segundo arquivo")
    void deveLimparParcialEmFalhaDeEscrita() {
        UUID executionId = UUID.randomUUID();
        var file1 = GenerationTestData.generatedFile("tests/a.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        var file2 = GenerationTestData.generatedFile("tests/b.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        var aiResult = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file1, file2);

        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(aiResult);
        when(validator.validate(any(), any(), any(), any(), any())).thenReturn(aiResult);

        doCallRealMethod()
                .doThrow(new GenerationWriteException("falha ao escrever segundo arquivo"))
                .when(fileWriter).write(any(), any(), any());

        TechnicalPlanResult plan = GenerationTestData.readyPlan(
                GenerationTestData.createAction("tests/a.spec.ts", PlanComponentType.TEST),
                GenerationTestData.createAction("tests/b.spec.ts", PlanComponentType.TEST)
        );

        assertThatThrownBy(() -> service.generate(executionId, GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), plan))
                .isInstanceOf(GenerationWriteException.class);

        Path firstFile = tempDir.resolve(executionId.toString()).resolve("files").resolve("tests/a.spec.ts");
        assertThat(Files.exists(firstFile)).isFalse();

        Path manifest = tempDir.resolve(executionId.toString()).resolve("manifest.json");
        assertThat(Files.exists(manifest)).isFalse();
    }

    @Test
    @DisplayName("Deve enviar somente o input sanitizado ao promptFactory")
    void deveEnviarSomenteInputSanitizado() {
        var aiResult = rawResultOneFile();
        SanitizedGenerationInput sanitized = mock(SanitizedGenerationInput.class);
        when(inputSanitizer.sanitize(any(), any(), any(), any())).thenReturn(sanitized);
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(aiResult);
        when(validator.validate(any(), any(), any(), any(), any())).thenReturn(aiResult);

        service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan());

        verify(promptFactory).createUserPrompt(sanitized);
    }

    @Test
    @DisplayName("Deve ser stateless entre execuções (executionIds independentes)")
    void deveSerStateless() {
        var aiResult = rawResultOneFile();
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(aiResult);
        when(validator.validate(any(), any(), any(), any(), any())).thenReturn(aiResult);

        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        GenerationResult r1 = service.generate(id1, GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan());
        GenerationResult r2 = service.generate(id2, GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan());

        assertThat(r1.executionId()).isEqualTo(id1);
        assertThat(r2.executionId()).isEqualTo(id2);
        assertThat(r1.executionId()).isNotEqualTo(r2.executionId());
    }

    @Test
    @DisplayName("Deve chamar cada provider no máximo uma vez")
    void deveChamarCadaProviderNoMaximoUmaVez() {
        var aiResult = rawResultOneFile();
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(aiResult);
        when(validator.validate(any(), any(), any(), any(), any())).thenReturn(aiResult);

        service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge(), onePlan());

        verify(primaryProvider, times(1)).gerarResposta(any(), any());
        verify(fallbackProvider, never()).gerarResposta(any(), any());
    }

    @Test
    @DisplayName("Deve marcar REUSE do plano com status SKIPPED sem escrever arquivo")
    void deveMarcarReuseComoSkipped() {
        TechnicalPlanResult plan = GenerationTestData.readyPlan(
                GenerationTestData.reuseAction("pages/LoginPage.ts", PlanComponentType.PAGE_OBJECT)
        );
        var aiResult = GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);
        when(primaryProvider.gerarResposta(any(), any())).thenReturn("{}");
        when(responseParser.parse(any())).thenReturn(aiResult);
        when(validator.validate(any(), any(), any(), any(), any())).thenReturn(aiResult);

        GenerationResult result = service.generate(UUID.randomUUID(), GenerationTestData.playwrightDiscovery(),
                GenerationTestData.validScenario(), GenerationTestData.completeKnowledge("pages/LoginPage.ts"), plan);

        assertThat(result.files()).hasSize(1);
        assertThat(result.files().get(0).status()).isEqualTo(GeneratedFileStatus.SKIPPED);
        assertThat(result.reusedFiles()).containsExactly("pages/LoginPage.ts");
    }

    // --- helpers ---

    private TechnicalPlanResult onePlan() {
        return GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST));
    }

    private GenerationResult rawResultOneFile() {
        var file = GenerationTestData.generatedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                GenerationTestData.PLAYWRIGHT_CONTENT, null, false);
        return GenerationTestData.aiResult(GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true, file);
    }

    private AiProvider mockProvider(String name) {
        AiProvider provider = mock(AiProvider.class);
        when(provider.getName()).thenReturn(name);
        return provider;
    }
}
