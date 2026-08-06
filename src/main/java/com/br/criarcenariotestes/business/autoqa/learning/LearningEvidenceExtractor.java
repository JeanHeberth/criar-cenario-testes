package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.apply.AppliedFile;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyFileStatus;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.TestExecutionSummary;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.failure.FailureFinding;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseCandidate;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningEvidence;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Extrai LearningEvidence exclusivamente dos 9 resultados estruturados já
 * produzidos pelas fases anteriores. Nunca acessa filesystem, nunca executa
 * comando, nunca relê stdout/stderr completo — apenas metadados já
 * estruturados (paths relativos, contagens, códigos).
 */
public class LearningEvidenceExtractor {

    private static final int MAX_PER_SOURCE = 20;

    public List<LearningEvidence> extract(
            ProjectDiscoveryResult discovery,
            ScenarioAnalysisResult scenario,
            ProjectKnowledgeResult knowledge,
            TechnicalPlanResult plan,
            GenerationResult generation,
            CodeReviewResult review,
            ApplyResult applyResult,
            ExecutionResult execution,
            FailureAnalysisResult failureAnalysis
    ) {
        Objects.requireNonNull(discovery, "discovery must not be null");
        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(knowledge, "knowledge must not be null");
        Objects.requireNonNull(plan, "plan must not be null");
        Objects.requireNonNull(generation, "generation must not be null");
        Objects.requireNonNull(review, "review must not be null");
        Objects.requireNonNull(applyResult, "applyResult must not be null");
        Objects.requireNonNull(execution, "execution must not be null");
        Objects.requireNonNull(failureAnalysis, "failureAnalysis must not be null");

        List<LearningEvidence> out = new ArrayList<>();
        extractDiscovery(discovery, out);
        extractKnowledge(knowledge, out);
        extractGeneration(generation, out);
        extractReview(review, out);
        extractApply(applyResult, out);
        extractExecution(execution, out);
        extractFailureAnalysis(failureAnalysis, out);
        return List.copyOf(out);
    }

    private void extractDiscovery(ProjectDiscoveryResult discovery, List<LearningEvidence> out) {
        if (discovery.automationFramework() == null
                || discovery.automationFramework().name().equals("UNKNOWN")) {
            return;
        }
        String description = "Framework identificado: " + discovery.automationFramework()
                + " / linguagem: " + discovery.language();
        out.add(new LearningEvidence("discovery", "framework", discovery.automationFramework().name(),
                null, description, null, true));
    }

    private void extractKnowledge(ProjectKnowledgeResult knowledge, List<LearningEvidence> out) {
        for (ReuseCandidate candidate : limit(knowledge.reuseCandidates())) {
            if (candidate.componentPath() == null) continue;
            String description = candidate.reason() != null ? candidate.reason() : "Componente candidato a reuso";
            out.add(new LearningEvidence("knowledge", "reuse-candidate", candidate.componentPath(),
                    candidate.componentPath(), description, null, true));
        }
    }

    private void extractGeneration(GenerationResult generation, List<LearningEvidence> out) {
        for (String reusedFile : limit(generation.reusedFiles())) {
            if (reusedFile == null || reusedFile.isBlank()) continue;
            out.add(new LearningEvidence("generation", "reused-file", reusedFile, reusedFile,
                    "Arquivo reaproveitado durante a geração", null, true));
        }
    }

    private void extractReview(CodeReviewResult review, List<LearningEvidence> out) {
        for (String rule : limit(review.passedRules())) {
            if (rule == null || rule.isBlank()) continue;
            out.add(new LearningEvidence("review", "passed-rule", rule, null,
                    "Regra de review aprovada: " + rule, null, true));
        }
    }

    private void extractApply(ApplyResult applyResult, List<LearningEvidence> out) {
        for (AppliedFile file : limit(applyResult.files())) {
            if (file.status() != ApplyFileStatus.APPLIED) continue;
            out.add(new LearningEvidence("apply", "applied-file", file.relativePath(), file.relativePath(),
                    "Arquivo aplicado com sucesso (" + file.operation() + ")", null, true));
        }
    }

    private void extractExecution(ExecutionResult execution, List<LearningEvidence> out) {
        for (TestExecutionSummary summary : limit(execution.summaries())) {
            String framework = summary.framework() == null ? "desconhecido" : summary.framework();
            String description = summary.passed() + " passaram, " + summary.failed() + " falharam de "
                    + summary.total() + " testes (" + framework + ")";
            out.add(new LearningEvidence("execution", "test-summary", framework, null, description, null, true));
        }
    }

    private void extractFailureAnalysis(FailureAnalysisResult failureAnalysis, List<LearningEvidence> out) {
        for (FailureFinding finding : limit(failureAnalysis.findings())) {
            out.add(new LearningEvidence("failure-analysis", "finding", finding.code(), null,
                    finding.title() + " (" + finding.category() + ")", null, true));
        }
    }

    private <T> List<T> limit(List<T> values) {
        if (values == null || values.size() <= MAX_PER_SOURCE) {
            return values == null ? List.of() : values;
        }
        return values.subList(0, MAX_PER_SOURCE);
    }
}
