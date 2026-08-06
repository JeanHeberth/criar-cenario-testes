package com.br.criarcenariotestes.business.autoqa.executionapi.persistence;

import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaStage;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.snapshot.SanitizedProjectDiscoverySnapshot;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.snapshot.SanitizedProjectKnowledgeSnapshot;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyApproval;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyResult;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionApproval;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.CodeReviewResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Snapshot operacional necessário para reidratar AutoQaContext, cobrindo
 * somente Discovery..Apply (nunca Execution/FailureAnalysis/Learning — essas
 * três fases sempre rodam num único bloco atômico, nunca pausam entre si
 * para aprovação, então nunca precisam ser reidratadas isoladamente). Nunca
 * contém conteúdo de arquivo, stdout/stderr ou projectPath — ver
 * AutoQaContextSnapshotMapper para as regras de sanitização aplicadas antes
 * de persistir. toString()/equals()/hashCode() customizados propositalmente.
 */
@Document(collection = "autoqa_execution_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class AutoQaExecutionSnapshot {

    @Id
    private String id;

    @Indexed(unique = true)
    private UUID executionId;

    private AutoQaStage lastCompletedStage;

    private SanitizedProjectDiscoverySnapshot discovery;
    private ScenarioAnalysisResult scenarioAnalysis;
    private SanitizedProjectKnowledgeSnapshot projectKnowledge;
    private TechnicalPlanResult technicalPlan;
    private GenerationResult generation;
    private CodeReviewResult codeReview;
    private ApplyApproval applyApproval;
    private ApplyResult apply;
    private ExecutionApproval executionApproval;

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;

    public static AutoQaExecutionSnapshot createNew(UUID executionId, Instant now) {
        AutoQaExecutionSnapshot snapshot = new AutoQaExecutionSnapshot();
        snapshot.executionId = executionId;
        snapshot.createdAt = now;
        snapshot.updatedAt = now;
        return snapshot;
    }

    @Override
    public String toString() {
        return "AutoQaExecutionSnapshot{executionId=" + executionId + ", lastCompletedStage=" + lastCompletedStage + "}";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AutoQaExecutionSnapshot that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
