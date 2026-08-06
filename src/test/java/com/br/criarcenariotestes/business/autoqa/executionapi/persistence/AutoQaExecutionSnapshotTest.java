package com.br.criarcenariotestes.business.autoqa.executionapi.persistence;

import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Version;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AutoQaExecutionSnapshot - Testes Unitários")
class AutoQaExecutionSnapshotTest {

    @Test
    @DisplayName("createNew deve inicializar sem nenhuma fase preenchida")
    void createNewDeveInicializarVazio() {
        UUID executionId = UUID.randomUUID();
        Instant now = Instant.now();

        AutoQaExecutionSnapshot snapshot = AutoQaExecutionSnapshot.createNew(executionId, now);

        assertThat(snapshot.getExecutionId()).isEqualTo(executionId);
        assertThat(snapshot.getLastCompletedStage()).isNull();
        assertThat(snapshot.getDiscovery()).isNull();
        assertThat(snapshot.getScenarioAnalysis()).isNull();
        assertThat(snapshot.getProjectKnowledge()).isNull();
        assertThat(snapshot.getTechnicalPlan()).isNull();
        assertThat(snapshot.getGeneration()).isNull();
        assertThat(snapshot.getCodeReview()).isNull();
        assertThat(snapshot.getApplyApproval()).isNull();
        assertThat(snapshot.getApply()).isNull();
        assertThat(snapshot.getExecutionApproval()).isNull();
        assertThat(snapshot.getCreatedAt()).isEqualTo(now);
        assertThat(snapshot.getVersion()).isNull();
    }

    @Test
    @DisplayName("Construtor sem argumentos deve existir (exigido pelo Spring Data Mongo)")
    void construtorSemArgumentosDeveExistir() {
        assertThat(new AutoQaExecutionSnapshot()).isNotNull();
    }

    @Test
    @DisplayName("Campo version deve estar anotado com @Version")
    void campoVersionDeveEstarAnotado() throws NoSuchFieldException {
        Field field = AutoQaExecutionSnapshot.class.getDeclaredField("version");
        assertThat(field.getAnnotation(Version.class)).isNotNull();
    }

    @Test
    @DisplayName("toString não deve expor os dados internos das fases")
    void toStringNaoDeveExporDadosDasFases() {
        AutoQaExecutionSnapshot snapshot = AutoQaExecutionSnapshot.createNew(UUID.randomUUID(), Instant.now());
        snapshot.setLastCompletedStage(AutoQaStage.PLANNING);
        String repr = snapshot.toString();
        assertThat(repr).doesNotContain("discovery=").doesNotContain("scenarioAnalysis=").doesNotContain("technicalPlan=");
    }

    @Test
    @DisplayName("equals/hashCode devem se basear no id do Mongo")
    void equalsHashCodeDevemSeBasearNoId() {
        AutoQaExecutionSnapshot a = AutoQaExecutionSnapshot.createNew(UUID.randomUUID(), Instant.now());
        AutoQaExecutionSnapshot b = AutoQaExecutionSnapshot.createNew(UUID.randomUUID(), Instant.now());
        a.setId("mesmo-id");
        b.setId("mesmo-id");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
