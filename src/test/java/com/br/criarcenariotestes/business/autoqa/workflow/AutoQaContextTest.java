package com.br.criarcenariotestes.business.autoqa.workflow;

import com.br.criarcenariotestes.business.autoqa.model.context.WorkflowIssue;
import com.br.criarcenariotestes.business.autoqa.model.context.WorkflowLog;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaMode;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutoQaStatus;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.request.AutoQaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AutoQaContext")
class AutoQaContextTest {

    private AutoQaContext context;

    @BeforeEach
    void setUp() {
        AutoQaRequest request = new AutoQaRequest(
                "Login Test", null, "Cenário de login",
                "/tmp/project", AutomationFramework.PLAYWRIGHT,
                AutomationLanguage.TYPESCRIPT, null,
                AutoQaMode.GENERATE_FOR_REVIEW, false, false
        );
        context = new AutoQaContext(request);
    }

    @Test
    @DisplayName("deve ter UUID não nulo ao ser criado")
    void hasUuidOnCreation() {
        assertThat(context.getExecutionId()).isNotNull();
        assertThat(context.executionIdAsString()).isNotBlank();
    }

    @Test
    @DisplayName("status inicial deve ser CREATED")
    void initialStatusIsCreated() {
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.CREATED);
    }

    @Test
    @DisplayName("startedAt deve ser preenchido na criação")
    void startedAtIsSet() {
        assertThat(context.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("dois contextos devem ter UUIDs diferentes")
    void uniqueExecutionIds() {
        AutoQaContext other = new AutoQaContext(context.getRequest());
        assertThat(context.getExecutionId()).isNotEqualTo(other.getExecutionId());
    }

    @Test
    @DisplayName("updateStatus deve atualizar o status e adicionar log")
    void updateStatusChangesStatusAndAddsLog() {
        context.updateStatus(AutoQaStatus.DISCOVERING_PROJECT, "TEST_STEP");
        assertThat(context.getStatus()).isEqualTo(AutoQaStatus.DISCOVERING_PROJECT);
        assertThat(context.getWorkflowLogs())
                .anyMatch(l -> "TEST_STEP".equals(l.step()));
    }

    @Test
    @DisplayName("addIssue deve adicionar à lista de issues")
    void addIssueAppendsToList() {
        context.addIssue(WorkflowIssue.error("STEP", "ERR001", "Erro de teste"));
        assertThat(context.getIssues()).hasSize(1);
        assertThat(context.getIssues().get(0).code()).isEqualTo("ERR001");
    }

    @Test
    @DisplayName("addIssue deve também adicionar log automaticamente")
    void addIssueAlsoAddsLog() {
        int logsBefore = context.getWorkflowLogs().size();
        context.addIssue(WorkflowIssue.error("STEP", "ERR001", "Erro de teste"));
        assertThat(context.getWorkflowLogs().size()).isGreaterThan(logsBefore);
    }

    @Test
    @DisplayName("hasBlockers deve retornar false quando não há blockers")
    void hasBlockersReturnsFalseWhenNone() {
        context.addIssue(WorkflowIssue.warning("STEP", "WARN", "Aviso", null));
        assertThat(context.hasBlockers()).isFalse();
    }

    @Test
    @DisplayName("hasBlockers deve retornar true quando há blocker")
    void hasBlockersReturnsTrueWhenPresent() {
        context.addIssue(WorkflowIssue.blocker("STEP", "BLOCKER001", "Blocker crítico"));
        assertThat(context.hasBlockers()).isTrue();
    }

    @Test
    @DisplayName("projectPathAsString deve retornar null quando path não foi definido")
    void projectPathAsStringReturnsNullWhenNotSet() {
        assertThat(context.projectPathAsString()).isNull();
    }

    @Test
    @DisplayName("projectPathAsString deve retornar String quando path está definido")
    void projectPathAsStringReturnsStringWhenSet() {
        context.setNormalizedProjectPath(Path.of("/tmp/meu-projeto"));
        assertThat(context.projectPathAsString()).isEqualTo("/tmp/meu-projeto");
    }

    @Test
    @DisplayName("normalizedProjectPath é acessível como Path")
    void normalizedProjectPathIsPath() {
        Path path = Path.of("/tmp/projeto");
        context.setNormalizedProjectPath(path);
        assertThat(context.getNormalizedProjectPath()).isEqualTo(path);
    }

    @Test
    @DisplayName("request deve ser acessível")
    void requestIsAccessible() {
        assertThat(context.getRequest()).isNotNull();
        assertThat(context.getRequest().scenarioText()).isEqualTo("Cenário de login");
    }

    @Test
    @DisplayName("log inicial deve registrar a criação")
    void initialLogRecordsCreation() {
        assertThat(context.getWorkflowLogs()).isNotEmpty();
        assertThat(context.getWorkflowLogs().get(0).step()).isEqualTo("INIT");
    }
}
