package com.br.criarcenariotestes.business.autoqa.knowledge.resolver;

import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseCandidate;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseConfidence;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Component
public class ReuseCandidateResolver {

    private static final Set<String> GENERIC_TERMS = Set.of(
            "cenario", "cenarios", "teste", "testes", "validar", "validacao", "fluxo", "processo",
            "sistema", "page", "object", "component", "componente",
            "helper", "helpers", "util", "utils", "service", "services", "data", "dados", "etapa", "passo",
            "acao", "acoes", "resultado", "objetivo", "precondicao", "precondicoes", "requisito", "regras",
            "regra", "api", "http", "app"
    );
    private static final int MAX_CANDIDATES = 10;

    public List<ReuseCandidate> resolve(List<ProjectComponent> components, ScenarioAnalysisResult scenarioAnalysis) {
        Objects.requireNonNull(components, "components must not be null");
        Objects.requireNonNull(scenarioAnalysis, "scenarioAnalysis must not be null");

        Set<String> scenarioTerms = scenarioTerms(scenarioAnalysis);
        List<ReuseCandidate> candidates = new ArrayList<>();
        for (ProjectComponent component : components.stream().sorted(Comparator.comparing(ProjectComponent::relativePath)).toList()) {
            Set<String> componentTerms = componentTerms(component);
            LinkedHashSet<String> matches = new LinkedHashSet<>();
            for (String term : componentTerms) {
                if (scenarioTerms.contains(term) && term.length() >= 3) {
                    matches.add(term);
                }
            }
            if (matches.isEmpty()) {
                continue;
            }
            ReuseConfidence confidence = confidence(component, matches);
            candidates.add(new ReuseCandidate(
                    component.relativePath(),
                    component.type(),
                    reason(component, matches),
                    confidence,
                    List.copyOf(matches)
            ));
        }
        return candidates.stream()
                .sorted(Comparator.comparing(ReuseCandidate::confidence).reversed().thenComparing(ReuseCandidate::componentPath))
                .limit(MAX_CANDIDATES)
                .toList();
    }

    private Set<String> scenarioTerms(ScenarioAnalysisResult scenarioAnalysis) {
        TreeSet<String> terms = new TreeSet<>();
        collectTerms(terms, scenarioAnalysis.title());
        collectTerms(terms, scenarioAnalysis.objective());
        scenarioAnalysis.preconditions().forEach(value -> collectTerms(terms, value));
        scenarioAnalysis.steps().forEach(step -> {
            collectTerms(terms, step.action());
            collectTerms(terms, step.expectedResult());
            step.dependencies().forEach(value -> collectTerms(terms, value));
        });
        scenarioAnalysis.businessRules().forEach(rule -> collectTerms(terms, rule.description()));
        scenarioAnalysis.entities().forEach(value -> collectTerms(terms, value));
        scenarioAnalysis.dependencies().forEach(value -> collectTerms(terms, value));
        scenarioAnalysis.warnings().forEach(value -> collectTerms(terms, value));
        return terms;
    }

    private Set<String> componentTerms(ProjectComponent component) {
        TreeSet<String> terms = new TreeSet<>();
        collectTerms(terms, component.relativePath());
        collectTerms(terms, component.name());
        component.declaredClasses().forEach(value -> collectTerms(terms, value));
        component.declaredMethods().forEach(value -> collectTerms(terms, value));
        component.imports().forEach(value -> collectTerms(terms, value));
        component.annotations().forEach(value -> collectTerms(terms, value));
        component.tags().forEach(value -> collectTerms(terms, value));
        return terms;
    }

    private void collectTerms(Set<String> terms, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String normalized = normalize(value);
        for (String token : tokenize(normalized)) {
            token = synonym(token);
            if (token.length() >= 3 && !GENERIC_TERMS.contains(token)) {
                terms.add(token);
            }
        }
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replace('-', ' ')
                .replace('_', ' ')
                .toLowerCase(Locale.ROOT);
        return normalized;
    }

    private List<String> tokenize(String value) {
        return Arrays.stream(value.split("[^a-z0-9]+"))
                .filter(token -> !token.isBlank())
                .toList();
    }

    private String synonym(String token) {
        return switch (token) {
            case "usuario" -> "user";
            case "senha" -> "password";
            case "autenticacao" -> "auth";
            default -> token;
        };
    }

    private ReuseConfidence confidence(ProjectComponent component, Set<String> matches) {
        if (matches.size() >= 2) {
            return ReuseConfidence.HIGH;
        }
        if (component.type() == ComponentType.PAGE_OBJECT || component.type() == ComponentType.API_CLIENT || component.type() == ComponentType.FACTORY) {
            return ReuseConfidence.MEDIUM;
        }
        return ReuseConfidence.LOW;
    }

    private String reason(ProjectComponent component, Set<String> matches) {
        List<String> sample = new ArrayList<>(matches);
        String termSummary = sample.size() == 1 ? sample.getFirst() : sample.getFirst() + ", " + sample.get(1);
        return "Componente relacionado aos termos " + termSummary + " presente no cenário";
    }
}
