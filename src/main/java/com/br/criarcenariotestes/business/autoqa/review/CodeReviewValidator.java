package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewIssue;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewSuggestion;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.review.exception.CodeReviewValidationException;
import com.br.criarcenariotestes.business.autoqa.security.PadroesDeConteudoProibido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Valida a resposta BRUTA da IA (CodeReviewAiResponse) isoladamente: coerência interna,
 * ausência de conteúdo perigoso, referência apenas a arquivos reais. Não conhece a fusão
 * com issues estáticas (isso é responsabilidade do CodeReviewService/ReviewSummaryBuilder).
 * Nunca modifica o objeto recebido — aceita (retorna a mesma instância) ou lança
 * CodeReviewValidationException.
 */
@Component
public class CodeReviewValidator {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewValidator.class);
    private static final Pattern MARKDOWN_FENCE = Pattern.compile("```");
    private static final Pattern DIFF_MARKER = Pattern.compile("(?m)^(---|\\+\\+\\+|@@ )");
    private static final Pattern ABSOLUTE_UNIX = Pattern.compile("^/");
    private static final Pattern ABSOLUTE_WINDOWS = Pattern.compile("(?i)^[A-Za-z]:\\\\");
    private static final Pattern ABSOLUTE_UNC = Pattern.compile("^\\\\\\\\");
    private static final Pattern FILE_URI = Pattern.compile("(?i)^file://");

    public CodeReviewAiResponse validate(CodeReviewAiResponse response,
                                          ProjectDiscoveryResult discovery,
                                          ScenarioAnalysisResult scenario,
                                          ProjectKnowledgeResult knowledge,
                                          TechnicalPlanResult plan,
                                          GenerationResult generation) {
        if (response == null) throw new CodeReviewValidationException("response must not be null");
        if (discovery == null) throw new CodeReviewValidationException("discovery must not be null");
        if (plan == null) throw new CodeReviewValidationException("plan must not be null");
        if (generation == null) throw new CodeReviewValidationException("generation must not be null");
        if (response.status() == null) throw new CodeReviewValidationException("status ausente");

        Set<String> reviewableCreateOrUpdatePaths = new HashSet<>();
        for (GeneratedFile file : generation.files()) {
            if (file == null) continue;
            if (file.operation() == GeneratedFileOperation.CREATE || file.operation() == GeneratedFileOperation.UPDATE) {
                reviewableCreateOrUpdatePaths.add(file.relativePath());
            }
        }

        List<CodeReviewAiResponse.AiFileReview> arquivosSaneados = new ArrayList<>();
        for (int i = 0; i < response.files().size(); i++) {
            CodeReviewAiResponse.AiFileReview fileReview = response.files().get(i);
            if (fileReview == null) throw new CodeReviewValidationException("files[" + i + "] nulo");
            String path = fileReview.relativePath();
            if (path == null || path.isBlank()) throw new CodeReviewValidationException("files[" + i + "].relativePath ausente");
            if (!reviewableCreateOrUpdatePaths.contains(path)) {
                throw new CodeReviewValidationException("Arquivo revisado não existe na GenerationResult (ou não é CREATE/UPDATE): " + path);
            }
            if (fileReview.status() == null) throw new CodeReviewValidationException("files[" + i + "].status ausente");

            List<ReviewIssue> issuesSaneados = sanearIssues(fileReview.issues(), path);
            validateSuggestions(fileReview.suggestions());
            arquivosSaneados.add(new CodeReviewAiResponse.AiFileReview(
                    fileReview.relativePath(), fileReview.status(), issuesSaneados,
                    fileReview.suggestions(), fileReview.passedRules(), fileReview.skippedRules(),
                    fileReview.confidence(), fileReview.valid()));
        }

        List<ReviewIssue> globaisSaneados = sanearIssues(response.globalIssues(), null);
        validateSuggestions(response.suggestions());
        validateNoInconsistentDuplicates(globaisSaneados);

        // Sem nada a sanear, devolve a instância recebida: reconstruir por
        // reconstruir esconderia, na leitura, que a resposta passou intacta.
        boolean saneouAlgo = !globaisSaneados.equals(response.globalIssues())
                || !arquivosSaneados.equals(response.files());
        CodeReviewAiResponse resultado = saneouAlgo
                ? new CodeReviewAiResponse(arquivosSaneados, globaisSaneados, response.suggestions(),
                        response.passedRules(), response.skippedRules(), response.warnings(),
                        response.status(), response.confidence(), response.humanReviewRequired(), response.valid())
                : response;
        if (saneouAlgo) {
            log.warn("Resposta da revisão saneada: achados malformados foram descartados ou normalizados.");
        }

        // As checagens de coerência rodam sobre o que SOBROU. Rodá-las na
        // resposta crua faria um achado já descartado (nulo, por exemplo)
        // estourar aqui — trocando um tudo-ou-nada por outro.
        resultado.files().forEach(this::validateFileStatusCoherence);
        validateGlobalStatusCoherence(resultado);

        return resultado;
    }

    /**
     * Saneia os achados em vez de derrubar a revisão inteira.
     *
     * <p>Observado em produção: a revisora devolveu um achado EMPTY_METHOD com
     * {@code line} inválida e o validador lançou exceção, descartando junto
     * quatro erros de compilação legítimos e acionáveis. Um número de linha
     * ruim num achado não invalida os outros.
     *
     * <p>Três tratamentos, por natureza do defeito:
     * <ul>
     *   <li>METADADO ruim (line &lt;= 0): normaliza para nulo e mantém o achado
     *       — a linha é acessório, o defeito apontado pode ser real;</li>
     *   <li>ESTRUTURA inutilizável (sem code/category/severity, path divergente):
     *       descarta AQUELE achado;</li>
     *   <li>SEGURANÇA (caminho ou texto perigoso): continua derrubando. Não é
     *       calibragem ruim do modelo, é conteúdo que não pode passar adiante.</li>
     * </ul>
     */
    private List<ReviewIssue> sanearIssues(List<ReviewIssue> issues, String expectedPath) {
        List<ReviewIssue> saneados = new ArrayList<>();
        for (int i = 0; i < issues.size(); i++) {
            ReviewIssue issue = issues.get(i);
            if (issue == null) {
                log.warn("Achado descartado: nulo na posição {}", i);
                continue;
            }
            if (issue.code() == null || issue.code().isBlank()
                    || issue.category() == null || issue.severity() == null) {
                log.warn("Achado descartado por estrutura incompleta. code='{}', category={}, severity={}",
                        issue.code(), issue.category(), issue.severity());
                continue;
            }
            if (issue.relativePath() != null) {
                rejectDangerousPath(issue.relativePath());
                if (expectedPath != null && !expectedPath.equals(issue.relativePath())) {
                    log.warn("Achado descartado: relativePath '{}' diverge do arquivo revisado '{}'",
                            issue.relativePath(), expectedPath);
                    continue;
                }
            }
            rejectDangerousText(issue.evidence(), "evidence");
            rejectDangerousText(issue.message(), "message");
            rejectDangerousText(issue.recommendation(), "recommendation");

            if (issue.line() != null && issue.line() <= 0) {
                log.warn("Achado com line inválida ({}) normalizada para nula. code='{}'",
                        issue.line(), issue.code());
                saneados.add(new ReviewIssue(issue.code(), issue.category(), issue.severity(),
                        issue.relativePath(), null, issue.message(), issue.evidence(),
                        issue.recommendation(), issue.blocking()));
                continue;
            }
            saneados.add(issue);
        }
        return List.copyOf(saneados);
    }

    private void validateSuggestions(List<ReviewSuggestion> suggestions) {
        for (ReviewSuggestion suggestion : suggestions) {
            if (suggestion == null) throw new CodeReviewValidationException("suggestion nula");
            if (suggestion.relativePath() != null) rejectDangerousPath(suggestion.relativePath());
            rejectDangerousText(suggestion.description(), "suggestion.description");
            rejectDangerousText(suggestion.rationale(), "suggestion.rationale");
        }
    }

    private void validateNoInconsistentDuplicates(List<ReviewIssue> issues) {
        Map<String, com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity> seen = new HashMap<>();
        for (ReviewIssue issue : issues) {
            String key = (issue.relativePath() == null ? "" : issue.relativePath()) + "::" + issue.code();
            var previous = seen.putIfAbsent(key, issue.severity());
            if (previous != null && previous != issue.severity()) {
                throw new CodeReviewValidationException("Issue duplicada com severidades inconsistentes: " + key);
            }
        }
    }

    private void validateFileStatusCoherence(CodeReviewAiResponse.AiFileReview fileReview) {
        boolean hasCritical = fileReview.issues().stream().anyMatch(i -> i.severity() == com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity.CRITICAL);
        boolean hasHigh = fileReview.issues().stream().anyMatch(i -> i.severity() == com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity.HIGH);

        switch (fileReview.status()) {
            case BLOCKED -> {
                if (!hasCritical) log.warn("AI status incoherence: BLOCKED sem issue CRITICAL. file={}", fileReview.relativePath());
            }
            case CHANGES_REQUIRED -> {
                if (hasCritical) log.warn("AI status incoherence: CHANGES_REQUIRED com CRITICAL. file={}", fileReview.relativePath());
                if (!hasHigh) log.warn("AI status incoherence: CHANGES_REQUIRED sem issue HIGH. file={}", fileReview.relativePath());
            }
            case APPROVED -> {
                if (hasCritical || hasHigh) log.warn("AI status incoherence: APPROVED com issue HIGH/CRITICAL. file={}", fileReview.relativePath());
            }
            case APPROVED_WITH_WARNINGS -> {
                if (hasCritical) log.warn("AI status incoherence: APPROVED_WITH_WARNINGS com CRITICAL. file={}", fileReview.relativePath());
                if (hasHigh) log.warn("AI status incoherence: APPROVED_WITH_WARNINGS com HIGH. file={}", fileReview.relativePath());
            }
            default -> { /* SKIPPED, INVALID */ }
        }
    }

    private void validateGlobalStatusCoherence(CodeReviewAiResponse response) {
        boolean hasCritical = response.globalIssues().stream().anyMatch(i -> i.severity() == com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity.CRITICAL)
                || response.files().stream().anyMatch(f -> f.issues().stream().anyMatch(i -> i.severity() == com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity.CRITICAL));
        boolean hasHigh = response.globalIssues().stream().anyMatch(i -> i.severity() == com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity.HIGH)
                || response.files().stream().anyMatch(f -> f.issues().stream().anyMatch(i -> i.severity() == com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity.HIGH));

        switch (response.status()) {
            case BLOCKED -> {
                if (!hasCritical) log.warn("AI global status incoherence: BLOCKED sem issue CRITICAL");
                if (!response.humanReviewRequired()) log.warn("AI global status incoherence: BLOCKED sem humanReviewRequired=true");
            }
            case CHANGES_REQUIRED -> {
                if (hasCritical) log.warn("AI global status incoherence: CHANGES_REQUIRED com CRITICAL");
                if (!hasHigh) log.warn("AI global status incoherence: CHANGES_REQUIRED sem issue HIGH");
            }
            case APPROVED -> {
                if (hasCritical || hasHigh) log.warn("AI global status incoherence: APPROVED com issue HIGH/CRITICAL");
            }
            case APPROVED_WITH_WARNINGS -> {
                if (hasCritical) log.warn("AI global status incoherence: APPROVED_WITH_WARNINGS com CRITICAL");
                if (hasHigh) log.warn("AI global status incoherence: APPROVED_WITH_WARNINGS com HIGH");
            }
            case INVALID -> {
                if (response.valid()) throw new CodeReviewValidationException("status INVALID deve ter valid=false");
            }
        }
    }

    private void rejectDangerousPath(String path) {
        if (ABSOLUTE_UNIX.matcher(path).find() || ABSOLUTE_WINDOWS.matcher(path).find()
                || ABSOLUTE_UNC.matcher(path).find() || FILE_URI.matcher(path).find()) {
            throw new CodeReviewValidationException("Caminho absoluto detectado na resposta da IA: " + path);
        }
    }

    private void rejectDangerousText(String text, String fieldName) {
        if (text == null || text.isBlank()) return;
        if (MARKDOWN_FENCE.matcher(text).find()) {
            throw new CodeReviewValidationException("Markdown fence (possível código completo) detectado em " + fieldName);
        }
        if (DIFF_MARKER.matcher(text).find()) {
            throw new CodeReviewValidationException("Marcador de diff/patch detectado em " + fieldName);
        }
        if (PadroesDeConteudoProibido.contemSegredoLiteral(text)) {
            throw new CodeReviewValidationException("Possível segredo detectado em " + fieldName);
        }
    }
}
