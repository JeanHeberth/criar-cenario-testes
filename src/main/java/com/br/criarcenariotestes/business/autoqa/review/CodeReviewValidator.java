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
import org.springframework.stereotype.Component;

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

    private static final Pattern MARKDOWN_FENCE = Pattern.compile("```");
    private static final Pattern DIFF_MARKER = Pattern.compile("(?m)^(---|\\+\\+\\+|@@ )");
    private static final Pattern ABSOLUTE_UNIX = Pattern.compile("^/");
    private static final Pattern ABSOLUTE_WINDOWS = Pattern.compile("(?i)^[A-Za-z]:\\\\");
    private static final Pattern ABSOLUTE_UNC = Pattern.compile("^\\\\\\\\");
    private static final Pattern FILE_URI = Pattern.compile("(?i)^file://");
    private static final Pattern SECRET_KEY_VALUE = Pattern.compile(
            "(?i)\\b(password|senha|secret|apikey|api_key|private_key)\\b\\s*[:=]\\s*[\"']?[^\\s,;\"'`]{3,}");

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

        for (int i = 0; i < response.files().size(); i++) {
            CodeReviewAiResponse.AiFileReview fileReview = response.files().get(i);
            if (fileReview == null) throw new CodeReviewValidationException("files[" + i + "] nulo");
            String path = fileReview.relativePath();
            if (path == null || path.isBlank()) throw new CodeReviewValidationException("files[" + i + "].relativePath ausente");
            if (!reviewableCreateOrUpdatePaths.contains(path)) {
                throw new CodeReviewValidationException("Arquivo revisado não existe na GenerationResult (ou não é CREATE/UPDATE): " + path);
            }
            if (fileReview.status() == null) throw new CodeReviewValidationException("files[" + i + "].status ausente");

            validateIssues(fileReview.issues(), path);
            validateSuggestions(fileReview.suggestions());
            validateFileStatusCoherence(fileReview);
        }

        validateIssues(response.globalIssues(), null);
        validateSuggestions(response.suggestions());
        validateNoInconsistentDuplicates(response.globalIssues());
        validateGlobalStatusCoherence(response);

        return response;
    }

    private void validateIssues(List<ReviewIssue> issues, String expectedPath) {
        for (int i = 0; i < issues.size(); i++) {
            ReviewIssue issue = issues.get(i);
            if (issue == null) throw new CodeReviewValidationException("issue nula na posição " + i);
            if (issue.code() == null || issue.code().isBlank()) throw new CodeReviewValidationException("issue sem code");
            if (issue.category() == null) throw new CodeReviewValidationException("issue sem category: " + issue.code());
            if (issue.severity() == null) throw new CodeReviewValidationException("issue sem severity: " + issue.code());
            if (issue.line() != null && issue.line() <= 0) {
                throw new CodeReviewValidationException("line deve ser positiva: " + issue.code());
            }
            if (issue.relativePath() != null) {
                rejectDangerousPath(issue.relativePath());
                if (expectedPath != null && !expectedPath.equals(issue.relativePath())) {
                    throw new CodeReviewValidationException("issue.relativePath diverge do arquivo revisado: " + issue.relativePath());
                }
            }
            rejectDangerousText(issue.evidence(), "evidence");
            rejectDangerousText(issue.message(), "message");
            rejectDangerousText(issue.recommendation(), "recommendation");
        }
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
                if (!hasCritical) throw new CodeReviewValidationException("BLOCKED requer issue CRITICAL: " + fileReview.relativePath());
            }
            case CHANGES_REQUIRED -> {
                if (hasCritical) throw new CodeReviewValidationException("CHANGES_REQUIRED não deveria ter CRITICAL (deveria ser BLOCKED): " + fileReview.relativePath());
                if (!hasHigh) throw new CodeReviewValidationException("CHANGES_REQUIRED requer issue HIGH: " + fileReview.relativePath());
            }
            case APPROVED -> {
                if (hasCritical || hasHigh) throw new CodeReviewValidationException("APPROVED não pode ter issue HIGH/CRITICAL: " + fileReview.relativePath());
            }
            case APPROVED_WITH_WARNINGS -> {
                if (hasCritical) throw new CodeReviewValidationException("APPROVED_WITH_WARNINGS não pode ter issue CRITICAL (deveria ser BLOCKED): " + fileReview.relativePath());
                if (hasHigh) throw new CodeReviewValidationException("APPROVED_WITH_WARNINGS não pode ter issue HIGH (deveria ser CHANGES_REQUIRED): " + fileReview.relativePath());
            }
            default -> { /* SKIPPED, INVALID: sem checagem estrutural adicional aqui */ }
        }
    }

    private void validateGlobalStatusCoherence(CodeReviewAiResponse response) {
        boolean hasCritical = response.globalIssues().stream().anyMatch(i -> i.severity() == com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity.CRITICAL)
                || response.files().stream().anyMatch(f -> f.issues().stream().anyMatch(i -> i.severity() == com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity.CRITICAL));
        boolean hasHigh = response.globalIssues().stream().anyMatch(i -> i.severity() == com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity.HIGH)
                || response.files().stream().anyMatch(f -> f.issues().stream().anyMatch(i -> i.severity() == com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity.HIGH));

        switch (response.status()) {
            case BLOCKED -> {
                if (!hasCritical) throw new CodeReviewValidationException("status BLOCKED requer issue CRITICAL");
                if (!response.humanReviewRequired()) throw new CodeReviewValidationException("status BLOCKED requer humanReviewRequired=true");
            }
            case CHANGES_REQUIRED -> {
                if (hasCritical) throw new CodeReviewValidationException("status CHANGES_REQUIRED não deveria ter CRITICAL");
                if (!hasHigh) throw new CodeReviewValidationException("status CHANGES_REQUIRED requer issue HIGH");
            }
            case APPROVED -> {
                if (hasCritical || hasHigh) throw new CodeReviewValidationException("status APPROVED não pode ter issue HIGH/CRITICAL");
            }
            case APPROVED_WITH_WARNINGS -> {
                if (hasCritical) throw new CodeReviewValidationException("status APPROVED_WITH_WARNINGS não pode ter issue CRITICAL");
                if (hasHigh) throw new CodeReviewValidationException("status APPROVED_WITH_WARNINGS não pode ter issue HIGH");
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
        if (SECRET_KEY_VALUE.matcher(text).find()) {
            throw new CodeReviewValidationException("Possível segredo detectado em " + fieldName);
        }
    }
}
