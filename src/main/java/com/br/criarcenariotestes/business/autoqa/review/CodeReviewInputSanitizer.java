package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewIssue;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import com.br.criarcenariotestes.business.autoqa.security.PadroesDeConteudoProibido;
import java.util.regex.Pattern;

@Component
public class CodeReviewInputSanitizer {

    static final int MAX_FILES = 15;
    static final int MAX_CONTENT_LENGTH = 4_000;
    static final int MAX_COMPONENTS = 15;
    static final int MAX_STATIC_ISSUES = 30;

    private static final String REDACTED = "[REDACTED]";
    private static final Pattern AUTHORIZATION_BEARER = Pattern.compile("(?i)\\b(bearer\\s+)([a-z0-9._-]{8,})");
    private static final Pattern URL_CREDENTIALS = Pattern.compile(
            "(?i)\\b((?:[a-z][a-z0-9+.-]*://)(?:[^\\s/?#:@]+:))([^\\s/?#@]+)(@)");

    public SanitizedCodeReviewInput sanitize(ProjectDiscoveryResult discovery,
                                              ScenarioAnalysisResult scenario,
                                              ProjectKnowledgeResult knowledge,
                                              TechnicalPlanResult plan,
                                              List<GeneratedArtifactReader.ReadArtifact> artifacts,
                                              List<ReviewIssue> staticIssues) {
        Objects.requireNonNull(discovery, "discovery must not be null");
        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(knowledge, "knowledge must not be null");
        Objects.requireNonNull(plan, "plan must not be null");

        List<SanitizedCodeReviewInput.SanitizedReviewFile> files = artifacts == null ? List.of() :
                artifacts.stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(GeneratedArtifactReader.ReadArtifact::relativePath))
                        .limit(MAX_FILES)
                        .map(a -> new SanitizedCodeReviewInput.SanitizedReviewFile(
                                a.relativePath(),
                                a.operation() != null ? a.operation().name() : "UNKNOWN",
                                a.componentType() != null ? a.componentType().name() : "UNKNOWN",
                                redact(truncate(a.content()))
                        ))
                        .toList();

        List<SanitizedCodeReviewInput.SanitizedComponent> reusableComponents = knowledge.components() == null ? List.of() :
                knowledge.components().stream()
                        .filter(Objects::nonNull)
                        .filter(ProjectComponent::reusable)
                        .sorted(Comparator.comparing(ProjectComponent::relativePath))
                        .limit(MAX_COMPONENTS)
                        .map(c -> new SanitizedCodeReviewInput.SanitizedComponent(
                                c.relativePath(), c.type() != null ? c.type().name() : "UNKNOWN", c.name()
                        ))
                        .toList();

        List<SanitizedCodeReviewInput.SanitizedStaticIssue> sanitizedStaticIssues = staticIssues == null ? List.of() :
                staticIssues.stream()
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing((ReviewIssue i) -> safe(i.relativePath())).thenComparing(i -> safe(i.code())))
                        .limit(MAX_STATIC_ISSUES)
                        .map(i -> new SanitizedCodeReviewInput.SanitizedStaticIssue(
                                i.code(),
                                i.category() != null ? i.category().name() : "UNKNOWN",
                                i.severity() != null ? i.severity().name() : "UNKNOWN",
                                i.relativePath(),
                                i.message()
                        ))
                        .toList();

        return new SanitizedCodeReviewInput(
                discovery.getAutomationFramework(),
                discovery.getLanguage(),
                scenario.title(),
                scenario.objective(),
                plan.title(),
                plan.strategy(),
                knowledge.namingConvention(),
                files,
                reusableComponents,
                sanitizedStaticIssues
        );
    }

    private String redact(String content) {
        String sanitized = content;
        sanitized = URL_CREDENTIALS.matcher(sanitized).replaceAll("$1" + REDACTED + "$3");
        sanitized = AUTHORIZATION_BEARER.matcher(sanitized).replaceAll("$1" + REDACTED);
        sanitized = PadroesDeConteudoProibido.redigirSegredosLiterais(sanitized);
        return sanitized;
    }

    private String truncate(String content) {
        if (content == null) return null;
        return content.length() <= MAX_CONTENT_LENGTH ? content : content.substring(0, MAX_CONTENT_LENGTH);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
