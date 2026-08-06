package com.br.criarcenariotestes.business.autoqa.executionapi.orchestrator;

import com.br.criarcenariotestes.business.autoqa.executionapi.config.AutoQaProperties;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaAvailableAction;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaOperationStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaWorkflowStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaApprovalRecord;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AutoQaAvailableActionResolver - Testes Unitários")
class AutoQaAvailableActionResolverTest {

    private final AutoQaAvailableActionResolver resolver = new AutoQaAvailableActionResolver();

    @Test
    @DisplayName("CREATED deve permitir START")
    void createdPermiteStart() {
        Set<AutoQaAvailableAction> actions = resolver.resolve(documento(AutoQaWorkflowStatus.CREATED), propertiesHabilitadas());
        assertThat(actions).contains(AutoQaAvailableAction.START);
    }

    @Test
    @DisplayName("Operação em andamento (lock) não deve permitir start duplicado nem nenhuma outra ação")
    void operacaoEmAndamentoNaoPermiteNenhumaAcao() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.CREATED);
        document.setOperationStatus(AutoQaOperationStatus.IN_PROGRESS);

        Set<AutoQaAvailableAction> actions = resolver.resolve(document, propertiesHabilitadas());

        assertThat(actions).doesNotContain(AutoQaAvailableAction.START);
        assertThat(actions).isEmpty();
    }

    @Test
    @DisplayName("WAITING_GENERATION_APPROVAL deve permitir GENERATE")
    void waitingGenerationApprovalPermiteGenerate() {
        Set<AutoQaAvailableAction> actions = resolver.resolve(documento(AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL), propertiesHabilitadas());
        assertThat(actions).contains(AutoQaAvailableAction.GENERATE);
    }

    @Test
    @DisplayName("WAITING_APPLY_APPROVAL sem aprovação deve permitir APPROVE_FILE_UPDATE, não APPLY")
    void waitingApplyApprovalSemAprovacaoPermiteAprovar() {
        Set<AutoQaAvailableAction> actions = resolver.resolve(documento(AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL), propertiesHabilitadas());
        assertThat(actions).contains(AutoQaAvailableAction.APPROVE_FILE_UPDATE);
        assertThat(actions).doesNotContain(AutoQaAvailableAction.APPLY);
    }

    @Test
    @DisplayName("WAITING_APPLY_APPROVAL com aprovação registrada deve permitir APPLY")
    void waitingApplyApprovalComAprovacaoPermiteAplicar() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
        document.getApprovals().add(new AutoQaApprovalRecord("APPLY", "qa.lead", Instant.now(), true));

        Set<AutoQaAvailableAction> actions = resolver.resolve(document, propertiesHabilitadas());

        assertThat(actions).contains(AutoQaAvailableAction.APPLY);
    }

    @Test
    @DisplayName("WAITING_EXECUTION_APPROVAL sem aprovação deve permitir APPROVE_EXECUTION, não EXECUTE")
    void waitingExecutionApprovalSemAprovacaoPermiteAprovar() {
        Set<AutoQaAvailableAction> actions = resolver.resolve(documento(AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL), propertiesHabilitadas());
        assertThat(actions).contains(AutoQaAvailableAction.APPROVE_EXECUTION);
        assertThat(actions).doesNotContain(AutoQaAvailableAction.EXECUTE);
    }

    @Test
    @DisplayName("WAITING_EXECUTION_APPROVAL com aprovação registrada deve permitir EXECUTE")
    void waitingExecutionApprovalComAprovacaoPermiteExecutar() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
        document.getApprovals().add(new AutoQaApprovalRecord("EXECUTION", "qa.lead", Instant.now(), true));

        Set<AutoQaAvailableAction> actions = resolver.resolve(document, propertiesHabilitadas());

        assertThat(actions).contains(AutoQaAvailableAction.EXECUTE);
    }

    @Test
    @DisplayName("COMPLETED não deve permitir nenhuma ação sensível")
    void completedNaoPermiteAcaoSensivel() {
        Set<AutoQaAvailableAction> actions = resolver.resolve(documento(AutoQaWorkflowStatus.COMPLETED), propertiesHabilitadas());
        assertThat(actions).doesNotContain(AutoQaAvailableAction.APPLY, AutoQaAvailableAction.EXECUTE,
                AutoQaAvailableAction.START, AutoQaAvailableAction.GENERATE);
    }

    @Test
    @DisplayName("COMPLETED deve permitir ações de visualização")
    void completedPermiteVisualizacao() {
        Set<AutoQaAvailableAction> actions = resolver.resolve(documento(AutoQaWorkflowStatus.COMPLETED), propertiesHabilitadas());
        assertThat(actions).contains(AutoQaAvailableAction.VIEW_GENERATED_FILES, AutoQaAvailableAction.VIEW_LEARNING,
                AutoQaAvailableAction.VIEW_LOGS);
    }

    @Test
    @DisplayName("CANCELLED não deve permitir retomada (sem CONTINUE)")
    void cancelledNaoPermiteRetomada() {
        Set<AutoQaAvailableAction> actions = resolver.resolve(documento(AutoQaWorkflowStatus.CANCELLED), propertiesHabilitadas());
        assertThat(actions).doesNotContain(AutoQaAvailableAction.CONTINUE, AutoQaAvailableAction.START);
    }

    @Test
    @DisplayName("CANCELLED sem nenhuma ação disponível deve retornar NONE")
    void cancelledRetornaNone() {
        Set<AutoQaAvailableAction> actions = resolver.resolve(documento(AutoQaWorkflowStatus.CANCELLED), propertiesHabilitadas());
        assertThat(actions).containsExactly(AutoQaAvailableAction.NONE);
    }

    @Test
    @DisplayName("FAILED deve permitir CONTINUE")
    void failedPermiteContinue() {
        Set<AutoQaAvailableAction> actions = resolver.resolve(documento(AutoQaWorkflowStatus.FAILED), propertiesHabilitadas());
        assertThat(actions).contains(AutoQaAvailableAction.CONTINUE);
    }

    @Test
    @DisplayName("Não-terminal sempre deve permitir CANCEL")
    void naoTerminalSemprePermiteCancel() {
        Set<AutoQaAvailableAction> actions = resolver.resolve(documento(AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL), propertiesHabilitadas());
        assertThat(actions).contains(AutoQaAvailableAction.CANCEL);
    }

    @Test
    @DisplayName("allowFileApplication=false deve remover APPLY mesmo com aprovação registrada")
    void allowFileApplicationFalseRemoveApply() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
        document.getApprovals().add(new AutoQaApprovalRecord("APPLY", "qa.lead", Instant.now(), true));
        AutoQaProperties properties = propertiesHabilitadas();
        properties.setAllowFileApplication(false);

        Set<AutoQaAvailableAction> actions = resolver.resolve(document, properties);

        assertThat(actions).doesNotContain(AutoQaAvailableAction.APPLY);
    }

    @Test
    @DisplayName("allowCommandExecution=false deve remover EXECUTE mesmo com aprovação registrada")
    void allowCommandExecutionFalseRemoveExecute() {
        AutoQaExecutionDocument document = documento(AutoQaWorkflowStatus.WAITING_EXECUTION_APPROVAL);
        document.getApprovals().add(new AutoQaApprovalRecord("EXECUTION", "qa.lead", Instant.now(), true));
        AutoQaProperties properties = propertiesHabilitadas();
        properties.setAllowCommandExecution(false);

        Set<AutoQaAvailableAction> actions = resolver.resolve(document, properties);

        assertThat(actions).doesNotContain(AutoQaAvailableAction.EXECUTE);
    }

    @Test
    @DisplayName("sensitiveActionsEnabled=false deve remover APPLY e EXECUTE")
    void sensitiveActionsDisabledRemoveApplyEExecute() {
        AutoQaExecutionDocument applyDoc = documento(AutoQaWorkflowStatus.WAITING_APPLY_APPROVAL);
        applyDoc.getApprovals().add(new AutoQaApprovalRecord("APPLY", "qa.lead", Instant.now(), true));
        AutoQaProperties properties = propertiesHabilitadas();
        properties.setSensitiveActionsEnabled(false);

        assertThat(resolver.resolve(applyDoc, properties)).doesNotContain(AutoQaAvailableAction.APPLY);
    }

    @Test
    @DisplayName("auto-qa.enabled=false deve retornar somente NONE, independente do status")
    void moduloDesabilitadoRetornaSomenteNone() {
        AutoQaProperties properties = propertiesHabilitadas();
        properties.setEnabled(false);

        Set<AutoQaAvailableAction> actions = resolver.resolve(documento(AutoQaWorkflowStatus.CREATED), properties);

        assertThat(actions).containsExactly(AutoQaAvailableAction.NONE);
    }

    @Test
    @DisplayName("Deve rejeitar documento nulo")
    void deveRejeitarDocumentoNulo() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> resolver.resolve(null, propertiesHabilitadas()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar properties nulo")
    void deveRejeitarPropertiesNulo() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> resolver.resolve(documento(AutoQaWorkflowStatus.CREATED), null))
                .isInstanceOf(NullPointerException.class);
    }

    private AutoQaExecutionDocument documento(AutoQaWorkflowStatus status) {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "cenário", "/projeto", Instant.now());
        document.setWorkflowStatus(status);
        return document;
    }

    private AutoQaProperties propertiesHabilitadas() {
        AutoQaProperties properties = new AutoQaProperties();
        properties.setEnabled(true);
        properties.setAllowFileApplication(true);
        properties.setAllowCommandExecution(true);
        properties.setSensitiveActionsEnabled(true);
        return properties;
    }
}
