package com.br.criarcenariotestes.business.autoqa.executionapi.mapper;

import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaExecutionListResponse;
import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaExecutionResponse;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaAvailableAction;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaStage;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaWorkflowStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaErrorRecord;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionDocument;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaWarningRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AutoQaExecutionResponseMapper - Testes Unitários")
class AutoQaExecutionResponseMapperTest {

    private final AutoQaExecutionResponseMapper mapper = new AutoQaExecutionResponseMapper();

    @Test
    @DisplayName("Deve mapear os campos básicos corretamente")
    void deveMapearCamposBasicos() {
        UUID executionId = UUID.randomUUID();
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(executionId, "cenário de teste", "/projeto", Instant.now());
        document.setWorkflowStatus(AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL);
        document.setCurrentStage(AutoQaStage.PLANNING);
        document.setProgress(40);
        document.setAvailableActions(Set.of(AutoQaAvailableAction.GENERATE));

        AutoQaExecutionResponse response = mapper.toResponse(document);

        assertThat(response.executionId()).isEqualTo(executionId);
        assertThat(response.scenario()).isEqualTo("cenário de teste");
        assertThat(response.status()).isEqualTo(AutoQaWorkflowStatus.WAITING_GENERATION_APPROVAL);
        assertThat(response.currentStage()).isEqualTo(AutoQaStage.PLANNING);
        assertThat(response.progress()).isEqualTo(40);
        assertThat(response.availableActions()).containsExactly(AutoQaAvailableAction.GENERATE);
    }

    @Test
    @DisplayName("NUNCA deve expor o projectPath, nem nulo nem preenchido")
    void nuncaDeveExporProjectPath() {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "cenário",
                "/projeto/sensivel/secreto", Instant.now());

        AutoQaExecutionResponse response = mapper.toResponse(document);

        assertThat(response.toString()).doesNotContain("/projeto/sensivel/secreto");
        for (var component : AutoQaExecutionResponse.class.getRecordComponents()) {
            assertThat(component.getName()).doesNotContainIgnoringCase("projectpath").doesNotContainIgnoringCase("path");
        }
    }

    @Test
    @DisplayName("Deve mapear warnings para AutoQaPublicWarning sem perder informação controlada")
    void deveMapearWarnings() {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "cenário", "/projeto", Instant.now());
        document.getWarnings().add(new AutoQaWarningRecord("OPERATIONAL_FAILURE", "status operacional", true));

        AutoQaExecutionResponse response = mapper.toResponse(document);

        assertThat(response.warnings()).hasSize(1);
        assertThat(response.warnings().get(0).code()).isEqualTo("OPERATIONAL_FAILURE");
        assertThat(response.warnings().get(0).blocking()).isTrue();
    }

    @Test
    @DisplayName("Deve mapear errors para AutoQaPublicError sanitizados (sem stacktrace)")
    void deveMapearErrors() {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "cenário", "/projeto", Instant.now());
        document.getErrors().add(new AutoQaErrorRecord("STAGE_FAILURE", "planning falhou"));

        AutoQaExecutionResponse response = mapper.toResponse(document);

        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().get(0).code()).isEqualTo("STAGE_FAILURE");
        assertThat(response.errors().get(0).message()).isEqualTo("planning falhou");
    }

    @Test
    @DisplayName("Deve mapear lastStageStarted, lastStageCompleted e attempt (rastreabilidade da retomada)")
    void deveMapearRastreabilidadeDaRetomada() {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "cenário", "/projeto", Instant.now());
        document.setLastStageStarted(AutoQaStage.LEARNING);
        document.setLastStageCompleted(AutoQaStage.FAILURE_ANALYSIS);
        document.setAttempt(2);

        AutoQaExecutionResponse response = mapper.toResponse(document);

        assertThat(response.lastStageStarted()).isEqualTo(AutoQaStage.LEARNING);
        assertThat(response.lastStageCompleted()).isEqualTo(AutoQaStage.FAILURE_ANALYSIS);
        assertThat(response.attempt()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve mapear timestamps de cancelamento")
    void deveMapearTimestampsDeCancelamento() {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "cenário", "/projeto", Instant.now());
        Instant cancelledAt = Instant.now();
        document.setCancelledAt(cancelledAt);
        document.setCancellationReason("motivo do cancelamento");

        AutoQaExecutionResponse response = mapper.toResponse(document);

        assertThat(response.cancelledAt()).isEqualTo(cancelledAt);
        assertThat(response.cancellationReason()).isEqualTo("motivo do cancelamento");
    }

    @Test
    @DisplayName("toListResponse deve mapear página do Spring Data para AutoQaExecutionListResponse")
    void toListResponseDeveMapearPagina() {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "cenário", "/projeto", Instant.now());
        var page = new PageImpl<>(List.of(document), PageRequest.of(0, 10), 1);

        AutoQaExecutionListResponse response = mapper.toListResponse(page);

        assertThat(response.items()).hasSize(1);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve rejeitar documento nulo")
    void deveRejeitarDocumentoNulo() {
        assertThatThrownBy(() -> mapper.toResponse(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar página nula")
    void deveRejeitarPaginaNula() {
        assertThatThrownBy(() -> mapper.toListResponse(null)).isInstanceOf(NullPointerException.class);
    }
}
