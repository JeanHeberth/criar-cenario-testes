package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationParseException;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationTechnicalException;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationValidationException;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationWriteException;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.*;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.FileOperation;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlannedFileAction;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanningStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class GenerationService {

    /** Arquivos inteiros não cabem no teto padrão dos demais estágios. */
    private static final int MAX_TOKENS_GERACAO = 16_000;

    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);

    private static final Set<AutomationFramework> SUPPORTED_FRAMEWORKS = EnumSet.of(
            AutomationFramework.PLAYWRIGHT, AutomationFramework.CYPRESS, AutomationFramework.SELENIDE,
            AutomationFramework.SELENIUM, AutomationFramework.REST_ASSURED, AutomationFramework.ROBOT_FRAMEWORK
    );

    private final AiProviderResolver aiProviderResolver;
    private final GenerationInputSanitizer inputSanitizer;
    private final GenerationPromptFactory promptFactory;
    private final GenerationResponseParser responseParser;
    private final GenerationValidator validator;
    private final GeneratedFileWriter fileWriter;
    private final IdiomasDoFramework idiomasDoFramework;
    private final GenerationHashService hashService;
    private final GenerationManifestWriter manifestWriter;

    private Path generatedBaseDir = Path.of(".auto-qa", "generated");

    public GenerationService(AiProviderResolver aiProviderResolver,
                              GenerationInputSanitizer inputSanitizer,
                              GenerationPromptFactory promptFactory,
                              GenerationResponseParser responseParser,
                              GenerationValidator validator,
                              GeneratedFileWriter fileWriter,
                              GenerationHashService hashService,
                              GenerationManifestWriter manifestWriter,
                              IdiomasDoFramework idiomasDoFramework) {
        this.aiProviderResolver = Objects.requireNonNull(aiProviderResolver);
        this.inputSanitizer = Objects.requireNonNull(inputSanitizer);
        this.promptFactory = Objects.requireNonNull(promptFactory);
        this.responseParser = Objects.requireNonNull(responseParser);
        this.validator = Objects.requireNonNull(validator);
        this.fileWriter = Objects.requireNonNull(fileWriter);
        this.idiomasDoFramework = Objects.requireNonNull(idiomasDoFramework);
        this.hashService = Objects.requireNonNull(hashService);
        this.manifestWriter = Objects.requireNonNull(manifestWriter);
    }

    /** Visível apenas para testes: permite isolar a escrita em um diretório temporário. */
    void setGeneratedBaseDir(Path generatedBaseDir) {
        this.generatedBaseDir = Objects.requireNonNull(generatedBaseDir, "generatedBaseDir must not be null");
    }

    public GenerationResult generate(UUID executionId,
                                      ProjectDiscoveryResult discovery,
                                      ScenarioAnalysisResult scenario,
                                      ProjectKnowledgeResult knowledge,
                                      TechnicalPlanResult plan) {
        return generate(executionId, discovery, scenario, knowledge, plan, java.util.List.of());
    }

    /**
     * @param correcoes erros da tentativa anterior. Vão para o topo do prompt.
     */
    public GenerationResult generate(UUID executionId,
                                      ProjectDiscoveryResult discovery,
                                      ScenarioAnalysisResult scenario,
                                      ProjectKnowledgeResult knowledge,
                                      TechnicalPlanResult plan,
                                      java.util.List<String> correcoes) {
        return generate(executionId, discovery, scenario, knowledge, plan, correcoes,
                java.util.List.of(), null);
    }

    /**
     * @param arquivosParaRegerar quando não vazio, só estes arquivos são pedidos
     *        à IA; os demais são reaproveitados de {@code geracaoAnterior}.
     * @param geracaoAnterior resultado da tentativa anterior, fonte dos arquivos
     *        que não precisam ser refeitos.
     */
    public GenerationResult generate(UUID executionId,
                                      ProjectDiscoveryResult discovery,
                                      ScenarioAnalysisResult scenario,
                                      ProjectKnowledgeResult knowledge,
                                      TechnicalPlanResult plan,
                                      java.util.List<String> correcoes,
                                      java.util.List<String> arquivosParaRegerar,
                                      GenerationResult geracaoAnterior) {
        return generate(executionId, discovery, scenario, knowledge, plan, correcoes,
                arquivosParaRegerar, geracaoAnterior, null);
    }

    /**
     * @param textoDoCenario texto ORIGINAL do cenário, fonte dos nomes de campo
     *        do contrato. A análise não serve: ela já é releitura da IA e
     *        traduz os nomes junto com a geração.
     */
    public GenerationResult generate(UUID executionId,
                                      ProjectDiscoveryResult discovery,
                                      ScenarioAnalysisResult scenario,
                                      ProjectKnowledgeResult knowledge,
                                      TechnicalPlanResult plan,
                                      java.util.List<String> correcoes,
                                      java.util.List<String> arquivosParaRegerar,
                                      GenerationResult geracaoAnterior,
                                      String textoDoCenario) {
        if (executionId == null) throw new IllegalArgumentException("executionId must not be null");
        if (discovery == null) throw new IllegalArgumentException("discovery must not be null");
        if (scenario == null) throw new IllegalArgumentException("scenario must not be null");
        if (knowledge == null) throw new IllegalArgumentException("knowledge must not be null");
        if (plan == null) throw new IllegalArgumentException("plan must not be null");

        if (scenario.status() == ScenarioAnalysisStatus.INVALID) {
            throw new GenerationValidationException("Cenário inválido não pode ser gerado");
        }
        if (knowledge.status() == KnowledgeStatus.FAILED) {
            throw new GenerationValidationException("Knowledge FAILED não pode ser gerado");
        }
        if (plan.status() == PlanningStatus.BLOCKED) {
            throw new GenerationValidationException("Plano BLOCKED não pode ser gerado");
        }
        if (plan.status() == PlanningStatus.INVALID) {
            throw new GenerationValidationException("Plano INVALID não pode ser gerado");
        }
        if (!SUPPORTED_FRAMEWORKS.contains(discovery.getAutomationFramework())) {
            throw new GenerationValidationException("Framework não suportado para geração: " + discovery.getAutomationFramework());
        }

        log.info("Generation started. executionId={}", executionId);

        TechnicalPlanResult planoDaTentativa = restringirAosArquivosComErro(plan, arquivosParaRegerar);
        // Derivado do plano COMPLETO, nunca do restrito: na regeração parcial o
        // cliente não está entre os arquivos pedidos, e sem isto o esqueleto
        // caía num caminho de exemplo que não existe.
        SanitizedGenerationInput sanitized = inputSanitizer.sanitize(discovery, scenario, knowledge,
                planoDaTentativa, correcoes, moduloDoCliente(plan), camposDoContrato(textoDoCenario));
        String systemPrompt = promptFactory.createSystemPrompt();
        String userPrompt = promptFactory.createUserPrompt(sanitized);

        AiProvider activeProvider = aiProviderResolver.getActiveProvider();
        AiProvider fallbackProvider = aiProviderResolver.getFallbackProvider();
        boolean sameProvider = sameProvider(activeProvider, fallbackProvider);

        log.debug("Generation provider selecionado: {}", safeProviderName(activeProvider));
        if (sameProvider) {
            log.warn("Generation fallback misconfigured. primaryProvider={}, fallbackProvider={}",
                    safeProviderName(activeProvider), safeProviderName(fallbackProvider));
        }

        // A validação usa o plano RESTRITO: numa regeração parcial os demais
        // arquivos são omitidos de propósito, e validar contra o plano inteiro
        // acusaria "Ação planejada omitida sem warning" para uma omissão que é
        // exatamente o comportamento pedido.
        GenerationResult aiResult = generateWithFallback(
                activeProvider, fallbackProvider, sameProvider, systemPrompt, userPrompt,
                discovery, scenario, knowledge, planoDaTentativa
        );

        // Já materialize recebe o plano INTEIRO: é dele que saem as entradas
        // REUSE/NONE, que continuam valendo para o resultado final.
        return materialize(executionId, discovery, plan, aiResult, arquivosParaRegerar, geracaoAnterior);
    }

    private GenerationResult generateWithFallback(AiProvider active, AiProvider fallback, boolean same,
                                                    String sys, String user,
                                                    ProjectDiscoveryResult discovery, ScenarioAnalysisResult scenario,
                                                    ProjectKnowledgeResult knowledge, TechnicalPlanResult plan) {
        try {
            GenerationResult result = generateWithProvider(active, sys, user, discovery, scenario, knowledge, plan);
            log.debug("Generation concluída. provider={}", safeProviderName(active));
            return result;
        } catch (GenerationValidationException e) {
            throw e;
        } catch (GenerationTechnicalException | GenerationParseException primary) {
            if (same) {
                log.error("Generation failed without fallback. provider={}, failureType={}, status=FAILED",
                        safeProviderName(active), primary.getClass().getSimpleName());
                throw new GenerationTechnicalException("Falha técnica na geração", primary);
            }
            log.warn("Generation primária falhou, tentando fallback. primaryProvider={}, fallbackProvider={}",
                    safeProviderName(active), safeProviderName(fallback));
            return generateWithFallbackProvider(active, fallback, sys, user, primary, discovery, scenario, knowledge, plan);
        }
    }

    private GenerationResult generateWithFallbackProvider(AiProvider active, AiProvider fallback,
                                                            String sys, String user, Exception primaryFailure,
                                                            ProjectDiscoveryResult discovery, ScenarioAnalysisResult scenario,
                                                            ProjectKnowledgeResult knowledge, TechnicalPlanResult plan) {
        try {
            GenerationResult result = generateWithProvider(fallback, sys, user, discovery, scenario, knowledge, plan);
            log.debug("Generation concluída via fallback. provider={}", safeProviderName(fallback));
            return result;
        } catch (GenerationValidationException e) {
            throw e;
        } catch (GenerationTechnicalException | GenerationParseException fallbackFailure) {
            log.error("Generation failed after fallback. primaryProvider={}, fallbackProvider={}, status=FAILED",
                    safeProviderName(active), safeProviderName(fallback));
            GenerationTechnicalException ex = new GenerationTechnicalException("Falha técnica na geração", fallbackFailure);
            ex.addSuppressed(primaryFailure);
            throw ex;
        }
    }

    private GenerationResult generateWithProvider(AiProvider provider, String systemPrompt, String userPrompt,
                                                    ProjectDiscoveryResult discovery, ScenarioAnalysisResult scenario,
                                                    ProjectKnowledgeResult knowledge, TechnicalPlanResult plan) {
        String response;
        try {
            // A geração emite ARQUIVOS INTEIROS — é a resposta mais longa do
            // pipeline. Observado: o Gemini truncou em MAX_TOKENS no meio de
            // uma string e derrubou a tentativa, com a chamada já paga.
            response = provider.gerarResposta(systemPrompt, userPrompt, MAX_TOKENS_GERACAO);
        } catch (RuntimeException e) {
            throw new GenerationTechnicalException("Falha técnica no provider " + provider.getName(), e);
        }
        GenerationResult parsed = responseParser.parse(response);
        return validator.validate(parsed, discovery, scenario, knowledge, plan);
    }

    private GenerationResult materialize(UUID executionId, ProjectDiscoveryResult discovery,
                                          TechnicalPlanResult plan, GenerationResult aiResult,
                                          java.util.List<String> arquivosParaRegerar,
                                          GenerationResult geracaoAnterior) {
        List<GeneratedFile> finalFiles = new ArrayList<>();
        List<String> reusedFiles = new ArrayList<>();
        List<Path> written = new ArrayList<>();

        for (PlannedFileAction action : plan.fileActions()) {
            if (action == null) continue;
            if (action.operation() == FileOperation.REUSE || action.operation() == FileOperation.NONE) {
                GeneratedFileOperation operation = action.operation() == FileOperation.REUSE
                        ? GeneratedFileOperation.REUSE : GeneratedFileOperation.NONE;
                finalFiles.add(new GeneratedFile(action.relativePath(), operation, action.componentType(), null,
                        "UTF-8", null, GeneratedFileStatus.SKIPPED, action.existingFile(), List.of(),
                        action.dependencies(), List.of()));
                if (operation == GeneratedFileOperation.REUSE) {
                    reusedFiles.add(action.relativePath());
                }
            }
        }

        // Uma NOVA tentativa começa com a área limpa. Sem isso, o CONTINUE
        // depois de uma geração bem-sucedida seguida de falha no review era um
        // beco sem saída: os arquivos da tentativa anterior continuavam lá e o
        // writer (corretamente) recusava sobrescrever, então toda retentativa
        // morria com "Arquivo já existe na área gerada". O guard do writer
        // protege contra colisão DENTRO de uma rodada e continua valendo.
        limparAreaGerada(generatedBaseDir, executionId, arquivosParaRegerar);

        try {
            for (GeneratedFile bruto : aiResult.files()) {
                // A correção vem ANTES da escrita e do hash: o que vai para
                // disco, para o manifesto e para a revisão precisa ser o mesmo
                // conteúdo, senão a checagem de integridade acusa divergência.
                GeneratedFile file = corrigirIdiomas(discovery, bruto);

                Path target = fileWriter.write(generatedBaseDir, executionId, file);
                written.add(target);
                GeneratedFileHash hash = hashService.sha256(file.content());
                finalFiles.add(new GeneratedFile(file.relativePath(), file.operation(), file.componentType(),
                        file.content(), file.encoding(), hash.hex(), GeneratedFileStatus.GENERATED,
                        file.existingFile(), file.reusedComponents(), file.dependencies(), file.warnings()));
            }
        } catch (GenerationWriteException e) {
            rollback(written);
            throw e;
        }

        finalFiles.addAll(arquivosPreservados(arquivosParaRegerar, geracaoAnterior, finalFiles));

        GenerationStatus finalStatus = aiResult.status();
        boolean valid = switch (finalStatus) {
            case COMPLETED, COMPLETED_WITH_WARNINGS -> true;
            case PARTIAL, FAILED -> false;
        };

        GenerationManifest manifest = buildManifest(executionId, discovery, plan, finalStatus, finalFiles, aiResult.warnings());
        try {
            manifestWriter.write(generatedBaseDir, executionId, manifest);
        } catch (GenerationWriteException e) {
            rollback(written);
            throw e;
        }

        String generatedRoot = generatedBaseDir.resolve(executionId.toString()).toString().replace('\\', '/');
        String manifestRelativePath = executionId + "/" + GenerationManifestWriter.MANIFEST_FILE_NAME;

        log.info("Generation finished. executionId={}, status={}, files={}", executionId, finalStatus, finalFiles.size());

        return new GenerationResult(
                executionId,
                discovery.getAutomationFramework().name(),
                discovery.getLanguage().name(),
                finalFiles,
                reusedFiles,
                aiResult.warnings(),
                generatedRoot,
                manifestRelativePath,
                finalStatus,
                aiResult.confidence(),
                valid
        );
    }

    private GenerationManifest buildManifest(UUID executionId, ProjectDiscoveryResult discovery, TechnicalPlanResult plan,
                                              GenerationStatus status, List<GeneratedFile> files, List<GenerationWarning> warnings) {
        List<GenerationManifest.GenerationManifestFile> manifestFiles = files.stream()
                .map(f -> new GenerationManifest.GenerationManifestFile(
                        f.relativePath(),
                        f.operation().name(),
                        f.componentType() != null ? f.componentType().name() : "UNKNOWN",
                        f.status() != null ? f.status().name() : "UNKNOWN",
                        f.sha256(),
                        f.existingFile()
                ))
                .toList();
        return new GenerationManifest(
                executionId,
                discovery.getAutomationFramework().name(),
                discovery.getLanguage().name(),
                plan.status().name(),
                status.name(),
                LocalDateTime.now().toString(),
                manifestFiles,
                warnings
        );
    }

    /**
     * Remove os arquivos da tentativa anterior desta execução — e SOMENTE dela.
     *
     * <p>A raiz é derivada do executionId e verificada com startsWith antes de
     * qualquer remoção: o caminho é construído aqui, nunca recebido de fora, e
     * a varredura não escapa de &lt;base&gt;/&lt;executionId&gt;/files.
     */
    private void limparAreaGerada(Path generatedBaseDir, UUID executionId,
                                   java.util.List<String> arquivosParaRegerar) {
        Path raizDaExecucao = generatedBaseDir.resolve(executionId.toString()).resolve("files").normalize();
        if (!java.nio.file.Files.isDirectory(raizDaExecucao)) {
            return;
        }

        // Numa regeração PARCIAL, apagar a área inteira destruiria os bytes dos
        // arquivos que estão sendo reaproveitados: o resultado ficaria com o
        // registro deles, mas a revisão não os acharia em disco
        // ("Arquivo gerado não encontrado"). Aqui só sai o que será refeito.
        if (arquivosParaRegerar != null && !arquivosParaRegerar.isEmpty()) {
            for (String relativo : arquivosParaRegerar) {
                try {
                    java.nio.file.Files.deleteIfExists(raizDaExecucao.resolve(relativo).normalize());
                } catch (java.io.IOException ignorado) {
                    // o writer acusa em seguida, com mensagem explícita
                }
            }
            return;
        }
        try (var caminhos = java.nio.file.Files.walk(raizDaExecucao)) {
            caminhos.filter(caminho -> caminho.startsWith(raizDaExecucao) && !caminho.equals(raizDaExecucao))
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(caminho -> {
                        try {
                            java.nio.file.Files.deleteIfExists(caminho);
                        } catch (java.io.IOException ignorado) {
                            // melhor esforço: se sobrar arquivo, o writer acusa e a
                            // tentativa falha com mensagem explícita, não em silêncio.
                        }
                    });
        } catch (java.io.IOException ignorado) {
            // idem: não é papel da limpeza derrubar a geração.
        }
    }

    private void rollback(List<Path> written) {
        for (Path path : written) {
            if (path == null) continue;
            try {
                java.nio.file.Files.deleteIfExists(path);
            } catch (java.io.IOException ignored) {
                // melhor esforço de limpeza; a área isolada pode ser removida integralmente depois
            }
        }
    }

    private boolean sameProvider(AiProvider a, AiProvider b) {
        return a == b || Objects.equals(normalizeProviderName(a), normalizeProviderName(b));
    }

    private String normalizeProviderName(AiProvider p) {
        return p == null ? null : safeProviderName(p).trim();
    }

    private String safeProviderName(AiProvider p) {
        if (p == null) return "unknown";
        String n = p.getName();
        return n == null || n.isBlank() ? p.getClass().getSimpleName() : n;
    }

    /**
     * Restringe o plano aos arquivos que precisam ser refeitos.
     *
     * <p>Com a lista vazia (primeira tentativa) devolve o plano inteiro. Numa
     * regeração, pedir os três arquivos de novo gasta token à toa e empurra a
     * resposta para o limite de saída — foi assim que o Gemini truncou no meio
     * de uma string e derrubou a tentativa.
     */
    private TechnicalPlanResult restringirAosArquivosComErro(TechnicalPlanResult plan,
                                                              java.util.List<String> arquivosParaRegerar) {
        if (arquivosParaRegerar == null || arquivosParaRegerar.isEmpty()) {
            return plan;
        }
        java.util.List<PlannedFileAction> restritas = plan.fileActions().stream()
                .filter(acao -> acao != null && arquivosParaRegerar.contains(acao.relativePath()))
                .toList();
        if (restritas.isEmpty()) {
            return plan;
        }
        log.info("Regeração parcial: pedindo {} de {} arquivos. arquivos={}",
                restritas.size(), plan.fileActions().size(), arquivosParaRegerar);
        return new TechnicalPlanResult(plan.title(), plan.strategy(), restritas, plan.components(),
                plan.reuseDecisions(), plan.risks(), plan.warnings(), plan.assumptions(),
                plan.constraints(), plan.requiredApprovals(), plan.status(), plan.confidence(), plan.valid());
    }

    /**
     * Arquivos da tentativa anterior que não foram refeitos nesta.
     *
     * <p>Sem eles o resultado sairia incompleto e a aderência ao plano acusaria
     * "arquivo planejado não foi gerado" — trocando o erro real por um falso.
     */
    private java.util.List<GeneratedFile> arquivosPreservados(java.util.List<String> arquivosParaRegerar,
                                                               GenerationResult geracaoAnterior,
                                                               java.util.List<GeneratedFile> jaIncluidos) {
        if (arquivosParaRegerar == null || arquivosParaRegerar.isEmpty() || geracaoAnterior == null) {
            return java.util.List.of();
        }
        java.util.Set<String> presentes = jaIncluidos.stream()
                .map(GeneratedFile::relativePath)
                .collect(java.util.stream.Collectors.toSet());

        java.util.List<GeneratedFile> preservados = geracaoAnterior.files().stream()
                .filter(arquivo -> arquivo != null && !presentes.contains(arquivo.relativePath()))
                .toList();
        if (!preservados.isEmpty()) {
            log.info("Regeração parcial: {} arquivo(s) reaproveitado(s) da tentativa anterior.", preservados.size());
        }
        return preservados;
    }

    /**
     * Aplica as correções de idioma do framework antes de o arquivo existir em
     * disco. Sem correção a aplicar, devolve o original intacto.
     */
    private GeneratedFile corrigirIdiomas(ProjectDiscoveryResult discovery, GeneratedFile file) {
        IdiomasDoFramework.Correcao correcao =
                idiomasDoFramework.aplicar(discovery.getAutomationFramework(), file.relativePath(), file.content());
        if (correcao.aplicadas().isEmpty()) {
            return file;
        }
        return new GeneratedFile(file.relativePath(), file.operation(), file.componentType(),
                correcao.conteudo(), file.encoding(), file.sha256(), file.status(),
                file.existingFile(), file.reusedComponents(), file.dependencies(), file.warnings());
    }

    /** Caminho de import do cliente, relativo ao spec e sem extensão. */
    private String moduloDoCliente(TechnicalPlanResult plan) {
        return plan.fileActions().stream()
                .map(acao -> acao.relativePath())
                .filter(caminho -> caminho != null && caminho.endsWith(".ts"))
                .filter(caminho -> caminho.toLowerCase(java.util.Locale.ROOT).contains("client"))
                .findFirst()
                .map(caminho -> {
                    String nome = caminho.substring(caminho.lastIndexOf('/') + 1);
                    return "./" + nome.substring(0, nome.length() - ".ts".length());
                })
                .orElse(null);
    }

    private java.util.List<String> camposDoContrato(String textoDoCenario) {
        var vocabulario = com.br.criarcenariotestes.business.autoqa.review.VocabularioDoContrato
                .doTexto(textoDoCenario);
        return vocabulario.utilizavel()
                ? vocabulario.campos().stream().sorted().toList()
                : java.util.List.of();
    }
}
