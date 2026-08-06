package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileStatus;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationConfidence;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.FileReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewConfidence;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewIssue;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;

import java.util.List;
import java.util.UUID;

public final class CodeReviewTestData {
    private CodeReviewTestData() {}

    public static ProjectDiscoveryResult discovery() {
        return GenerationTestData.playwrightDiscovery();
    }

    public static ScenarioAnalysisResult scenario() {
        return GenerationTestData.validScenario();
    }

    public static TechnicalPlanResult plan(String... createPaths) {
        var actions = java.util.Arrays.stream(createPaths)
                .map(p -> GenerationTestData.createAction(p, PlanComponentType.TEST))
                .toArray(com.br.criarcenariotestes.business.autoqa.model.planning.PlannedFileAction[]::new);
        return GenerationTestData.readyPlan(actions);
    }

    public static GenerationResult generation(UUID executionId, String... createPaths) {
        GeneratedFile[] files = java.util.Arrays.stream(createPaths)
                .map(p -> new GeneratedFile(p, GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                        GenerationTestData.PLAYWRIGHT_CONTENT, "UTF-8", "hash-" + p, GeneratedFileStatus.GENERATED,
                        false, List.of(), List.of(), List.of()))
                .toArray(GeneratedFile[]::new);
        return new GenerationResult(executionId, "PLAYWRIGHT", "TYPESCRIPT", List.of(files), List.of(), List.of(),
                ".auto-qa/generated/" + executionId, executionId + "/manifest.json",
                GenerationStatus.COMPLETED, GenerationConfidence.HIGH, true);
    }

    public static CodeReviewAiResponse approvedResponse(String... paths) {
        var files = java.util.Arrays.stream(paths)
                .map(p -> new CodeReviewAiResponse.AiFileReview(p, FileReviewStatus.APPROVED, List.of(), List.of(),
                        List.of(), List.of(), ReviewConfidence.HIGH, true))
                .toList();
        return new CodeReviewAiResponse(files, List.of(), List.of(), List.of(), List.of(), List.of(),
                ReviewStatus.APPROVED, ReviewConfidence.HIGH, false, true);
    }

    public static CodeReviewAiResponse responseWithIssue(String path, ReviewIssue issue, FileReviewStatus fileStatus, ReviewStatus globalStatus, boolean humanReviewRequired, boolean valid) {
        var fileReview = new CodeReviewAiResponse.AiFileReview(path, fileStatus, List.of(issue), List.of(),
                List.of(), List.of(), ReviewConfidence.HIGH, fileStatus != FileReviewStatus.BLOCKED);
        return new CodeReviewAiResponse(List.of(fileReview), List.of(), List.of(), List.of(), List.of(), List.of(),
                globalStatus, ReviewConfidence.HIGH, humanReviewRequired, valid);
    }
}
