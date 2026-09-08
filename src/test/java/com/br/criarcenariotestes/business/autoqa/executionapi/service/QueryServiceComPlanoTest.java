package com.br.criarcenariotestes.business.autoqa.executionapi.service;

import com.br.criarcenariotestes.business.autoqa.executionapi.mapper.AutoQaExecutionResponseMapper;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.*;
import com.br.criarcenariotestes.business.autoqa.model.planning.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Spec: o GET carrega o snapshot para devolver o plano.
 *
 * <p>Sem isto o campo existiria no contrato e viria sempre nulo — pior que não
 * existir, porque o front confiaria nele.
 */
class QueryServiceComPlanoTest {

    private final AutoQaExecutionRepository repository = mock(AutoQaExecutionRepository.class);
    private final AutoQaExecutionSnapshotRepository snapshots = mock(AutoQaExecutionSnapshotRepository.class);
    private final AutoQaExecutionQueryService service =
            new AutoQaExecutionQueryService(repository, new AutoQaExecutionResponseMapper(), snapshots);

    private final UUID id = UUID.randomUUID();

    private AutoQaExecutionDocument documento() {
        var doc = new AutoQaExecutionDocument();
        doc.setExecutionId(id);
        doc.setScenarioSummary("cenário");
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        return doc;
    }

    @Test
    void deveDevolverOPlanoDoSnapshot() {
        var acao = new PlannedFileAction("tests/api/auth/login.spec.ts", FileOperation.CREATE,
                PlanComponentType.TEST, "cobrir login", false, true,
                ApprovalRequirement.NONE, List.of(), List.of());
        var plano = new TechnicalPlanResult("Plano", "estratégia", List.of(acao), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                PlanningStatus.READY, PlanningConfidence.HIGH, true);
        var snapshot = new AutoQaExecutionSnapshot();
        snapshot.setTechnicalPlan(plano);

        when(repository.findByExecutionId(id)).thenReturn(Optional.of(documento()));
        when(snapshots.findByExecutionId(id)).thenReturn(Optional.of(snapshot));

        var resposta = service.get(id);

        assertThat(resposta.plan()).isNotNull();
        assertThat(resposta.plan().fileActions()).singleElement()
                .satisfies(a -> assertThat(a.relativePath()).isEqualTo("tests/api/auth/login.spec.ts"));
    }

    @Test
    void deveDevolverPlanoNuloQuandoAindaNaoHaSnapshot() {
        when(repository.findByExecutionId(id)).thenReturn(Optional.of(documento()));
        when(snapshots.findByExecutionId(id)).thenReturn(Optional.empty());

        assertThat(service.get(id).plan()).isNull();
    }
}
