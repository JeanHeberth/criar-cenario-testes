package com.br.criarcenariotestes.business.autoqa.executionapi.persistence;

import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaOperationStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaWorkflowStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Version;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AutoQaExecutionDocument - Testes Unitários")
class AutoQaExecutionDocumentTest {

    @Test
    @DisplayName("createNew deve inicializar estado padrão")
    void createNewDeveInicializarEstadoPadrao() {
        UUID executionId = UUID.randomUUID();
        Instant now = Instant.now();

        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(executionId, "cenário de teste", "/projeto/secreto", now);

        assertThat(document.getExecutionId()).isEqualTo(executionId);
        assertThat(document.getScenarioSummary()).isEqualTo("cenário de teste");
        assertThat(document.getProjectPath()).isEqualTo("/projeto/secreto");
        assertThat(document.getWorkflowStatus()).isEqualTo(AutoQaWorkflowStatus.CREATED);
        assertThat(document.getOperationStatus()).isEqualTo(AutoQaOperationStatus.IDLE);
        assertThat(document.getAttempt()).isZero();
        assertThat(document.getProgress()).isZero();
        assertThat(document.getCreatedAt()).isEqualTo(now);
        assertThat(document.getUpdatedAt()).isEqualTo(now);
        assertThat(document.getVersion()).isNull();
    }

    @Test
    @DisplayName("Coleções nunca devem ser nulas em uma instância nova")
    void colecoesNuncaDevemSerNulas() {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "c", "/p", Instant.now());
        assertThat(document.getStages()).isNotNull().isEmpty();
        assertThat(document.getApprovals()).isNotNull().isEmpty();
        assertThat(document.getWarnings()).isNotNull().isEmpty();
        assertThat(document.getErrors()).isNotNull().isEmpty();
        assertThat(document.getAvailableActions()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Construtor sem argumentos deve existir (exigido pelo Spring Data Mongo)")
    void construtorSemArgumentosDeveExistir() {
        AutoQaExecutionDocument document = new AutoQaExecutionDocument();
        assertThat(document).isNotNull();
    }

    @Test
    @DisplayName("Campo version deve estar anotado com @Version (optimistic locking)")
    void campoVersionDeveEstarAnotado() throws NoSuchFieldException {
        Field field = AutoQaExecutionDocument.class.getDeclaredField("version");
        assertThat(field.getAnnotation(Version.class)).isNotNull();
    }

    @Test
    @DisplayName("toString não deve expor projectPath")
    void toStringNaoDeveExporProjectPath() {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "c", "/projeto/sensivel/secreto", Instant.now());
        assertThat(document.toString()).doesNotContain("/projeto/sensivel/secreto");
    }

    @Test
    @DisplayName("toString não deve expor scenarioSummary")
    void toStringNaoDeveExporScenarioSummary() {
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "cenário detalhado sensível", "/p", Instant.now());
        assertThat(document.toString()).doesNotContain("cenário detalhado sensível");
    }

    @Test
    @DisplayName("toString deve conter apenas identificadores/status controlados")
    void toStringDeveConterApenasIdentificadores() {
        UUID executionId = UUID.randomUUID();
        AutoQaExecutionDocument document = AutoQaExecutionDocument.createNew(executionId, "c", "/p", Instant.now());
        assertThat(document.toString()).contains(executionId.toString()).contains("CREATED");
    }

    @Test
    @DisplayName("equals/hashCode devem se basear no id do Mongo, não nos dados operacionais")
    void equalsHashCodeDevemSeBasearNoId() {
        AutoQaExecutionDocument a = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "c1", "/p1", Instant.now());
        AutoQaExecutionDocument b = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "c2", "/p2", Instant.now());
        a.setId("mesmo-id");
        b.setId("mesmo-id");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("Instâncias sem id não devem ser iguais entre si")
    void instanciasSemIdNaoDevemSerIguais() {
        AutoQaExecutionDocument a = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "c", "/p", Instant.now());
        AutoQaExecutionDocument b = AutoQaExecutionDocument.createNew(UUID.randomUUID(), "c", "/p", Instant.now());
        assertThat(a).isNotEqualTo(b);
    }
}
