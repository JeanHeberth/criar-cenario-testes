package com.br.criarcenariotestes.business.autoqa.executionapi.mapper;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaSnapshotException;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaStage;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionSnapshot;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.snapshot.SanitizedProjectDiscoverySnapshot;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.snapshot.SanitizedProjectKnowledgeSnapshot;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Converte entre AutoQaContext (em memória, nunca persistido diretamente) e
 * AutoQaExecutionSnapshot (Mongo, até ApplyResult/ExecutionApproval). A
 * reidratação usa exclusivamente os métodos públicos registerXxx() do
 * AutoQaContext, na ordem real da cadeia de pré-condições — nunca reflection,
 * nunca campo privado, nunca setter genérico. Qualquer inconsistência do
 * snapshot (ordem impossível, dado ausente) vira AutoQaSnapshotException,
 * nunca é corrigida silenciosamente.
 */
@Component
public class AutoQaContextSnapshotMapper {

    public AutoQaExecutionSnapshot toSnapshot(AutoQaContext context, AutoQaExecutionSnapshot target,
                                               AutoQaStage lastCompletedStage, Instant now) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(now, "now must not be null");

        if (lastCompletedStage != null) {
            target.setLastCompletedStage(lastCompletedStage);
        }
        if (context.getProjectDiscoveryResult() != null) {
            target.setDiscovery(SanitizedProjectDiscoverySnapshot.from(context.getProjectDiscoveryResult()));
        }
        if (context.getScenarioAnalysisResult() != null) {
            target.setScenarioAnalysis(context.getScenarioAnalysisResult());
        }
        if (context.getProjectKnowledgeResult() != null) {
            target.setProjectKnowledge(SanitizedProjectKnowledgeSnapshot.from(context.getProjectKnowledgeResult()));
        }
        if (context.getTechnicalPlanResult() != null) {
            target.setTechnicalPlan(context.getTechnicalPlanResult());
        }
        if (context.getGenerationResult() != null) {
            target.setGeneration(sanitizeGeneration(context.getGenerationResult()));
        }
        if (context.getCodeReviewResult() != null) {
            target.setCodeReview(context.getCodeReviewResult());
        }
        if (context.getApplyApproval() != null) {
            target.setApplyApproval(context.getApplyApproval());
        }
        if (context.getApplyResult() != null) {
            target.setApply(context.getApplyResult());
        }
        if (context.getExecutionApproval() != null) {
            target.setExecutionApproval(context.getExecutionApproval());
        }
        target.setUpdatedAt(now);
        return target;
    }

    public AutoQaContext toContext(AutoQaExecutionSnapshot snapshot, String scenario, String projectPath) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        AutoQaContext context = AutoQaContext.create(scenario, projectPath);
        Path path = Path.of(projectPath);

        try {
            if (snapshot.getDiscovery() != null) {
                context.registerProjectDiscovery(snapshot.getDiscovery().toResult(path));
            }
            if (snapshot.getScenarioAnalysis() != null) {
                context.registerScenarioAnalysis(snapshot.getScenarioAnalysis());
            }
            if (snapshot.getProjectKnowledge() != null) {
                context.registerProjectKnowledge(snapshot.getProjectKnowledge().toResult(path));
            }
            if (snapshot.getTechnicalPlan() != null) {
                context.registerTechnicalPlan(snapshot.getTechnicalPlan());
            }
            if (snapshot.getGeneration() != null) {
                context.registerGeneration(snapshot.getGeneration());
            }
            if (snapshot.getCodeReview() != null) {
                context.registerCodeReview(snapshot.getCodeReview());
            }
            if (snapshot.getApplyApproval() != null) {
                context.registerApplyApproval(snapshot.getApplyApproval());
            }
            if (snapshot.getApply() != null) {
                context.registerApplyResult(snapshot.getApply());
            }
            if (snapshot.getExecutionApproval() != null) {
                context.registerExecutionApproval(snapshot.getExecutionApproval());
            }
        } catch (RuntimeException ex) {
            throw new AutoQaSnapshotException("Snapshot inconsistente ou incompleto para reidratação", ex);
        }
        return context;
    }

    private GenerationResult sanitizeGeneration(GenerationResult result) {
        List<GeneratedFile> sanitizedFiles = result.files().stream()
                .map(f -> new GeneratedFile(f.relativePath(), f.operation(), f.componentType(), null, f.encoding(),
                        f.sha256(), f.status(), f.existingFile(), f.reusedComponents(), f.dependencies(), f.warnings()))
                .toList();
        return new GenerationResult(result.executionId(), result.framework(), result.language(), sanitizedFiles,
                result.reusedFiles(), result.warnings(), result.generatedRoot(), result.manifestRelativePath(),
                result.status(), result.confidence(), result.valid());
    }
}
