package com.br.criarcenariotestes.business.autoqa.executionapi.persistence;

import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaAvailableAction;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaOperationStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaStage;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaWorkflowStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Documento público de histórico/dashboard de uma execução Auto QA. Nunca
 * contém prompt, resposta bruta de IA, credencial, stdout/stderr completo ou
 * conteúdo de arquivo — apenas metadados de controle. `projectPath` é
 * interno (necessário para os agentes operarem sobre o filesystem real) e
 * nunca é copiado para o DTO público (ver AutoQaExecutionResponseMapper).
 * toString()/equals()/hashCode() são customizados propositalmente — nunca
 * geração automática (@Data) sobre um documento com dados operacionais.
 */
@Document(collection = "autoqa_executions")
@Getter
@Setter
@NoArgsConstructor
public class AutoQaExecutionDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private UUID executionId;

    private String scenarioSummary;
    private String projectPath;
    /**
     * Canal informado pelo usuário na criação (WEB_UI, API, ...); null quando
     * ele deixou a dedução a cargo do discovery. Persistido porque o contexto
     * é reidratado do snapshot a cada estágio — sem guardar, o canal se perderia
     * entre o START e a análise de cenário.
     */
    private String automationType;
    /** Framework informado na criação; null quando deixado a cargo do discovery. */
    private String automationFramework;

    private AutoQaWorkflowStatus workflowStatus;
    private AutoQaStage currentStage;
    private AutoQaStage lastStageStarted;
    private AutoQaStage lastStageCompleted;
    private AutoQaOperationStatus operationStatus;
    private int attempt;
    private int progress;

    private List<AutoQaStageRecord> stages = new ArrayList<>();
    private List<AutoQaApprovalRecord> approvals = new ArrayList<>();
    private List<AutoQaWarningRecord> warnings = new ArrayList<>();
    private List<AutoQaErrorRecord> errors = new ArrayList<>();
    private Set<AutoQaAvailableAction> availableActions = new LinkedHashSet<>();

    private Instant createdAt;
    private Instant updatedAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant cancelledAt;
    private String cancellationReason;

    @Version
    private Long version;

    public static AutoQaExecutionDocument createNew(UUID executionId, String scenarioSummary, String projectPath, Instant now) {
        return createNew(executionId, scenarioSummary, projectPath, null, null, now);
    }

    public static AutoQaExecutionDocument createNew(UUID executionId, String scenarioSummary, String projectPath,
                                                    String automationType, String automationFramework, Instant now) {
        AutoQaExecutionDocument document = new AutoQaExecutionDocument();
        document.executionId = executionId;
        document.scenarioSummary = scenarioSummary;
        document.projectPath = projectPath;
        document.automationType = automationType;
        document.automationFramework = automationFramework;
        document.workflowStatus = AutoQaWorkflowStatus.CREATED;
        document.operationStatus = AutoQaOperationStatus.IDLE;
        document.attempt = 0;
        document.progress = 0;
        document.createdAt = now;
        document.updatedAt = now;
        return document;
    }

    @Override
    public String toString() {
        return "AutoQaExecutionDocument{executionId=" + executionId
                + ", workflowStatus=" + workflowStatus
                + ", currentStage=" + currentStage
                + ", operationStatus=" + operationStatus
                + "}";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AutoQaExecutionDocument that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
