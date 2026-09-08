package com.br.criarcenariotestes.business.autoqa.executionapi.mapper;

import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionDocument;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionSnapshot;
import com.br.criarcenariotestes.business.autoqa.model.planning.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec: o GET da execução devolve o plano técnico quando ele já existe.
 *
 * <p>Sem isso, o portão de aprovação é cego: o usuário clica em "Aprovar e
 * gerar código" sem ver o que será gerado. E as advertências da auditoria de
 * reuso, que existem para informar essa decisão, não chegam a ele.
 */
class PlanoNaRespostaTest {

    private final AutoQaExecutionResponseMapper mapper = new AutoQaExecutionResponseMapper();

    private AutoQaExecutionDocument documento() {
        AutoQaExecutionDocument doc = new AutoQaExecutionDocument();
        doc.setExecutionId(UUID.randomUUID());
        doc.setScenarioSummary("cenário");
        doc.setCreatedAt(Instant.now());
        doc.setUpdatedAt(Instant.now());
        return doc;
    }

    private TechnicalPlanResult plano() {
        var acao = new PlannedFileAction("tests/api/auth/login.spec.ts", FileOperation.CREATE,
                PlanComponentType.TEST, "cobrir autenticação", false, true,
                ApprovalRequirement.NONE, List.of(), List.of());
        var advertencia = new PlanningWarning("POSSIVEL_DUPLICACAO",
                "login.spec.ts planejado como CREATE, mas a auditoria indica ESTENDER", true);
        return new TechnicalPlanResult("Plano de login", "Cobrir 200/401/400", List.of(acao),
                List.of(), List.of(), List.of(), List.of(advertencia), List.of(), List.of(), List.of(),
                PlanningStatus.READY, PlanningConfidence.HIGH, true);
    }

    @Test
    void deveExporOPlanoQuandoJaExiste() {
        var snapshot = new AutoQaExecutionSnapshot();
        snapshot.setTechnicalPlan(plano());

        var resposta = mapper.toResponse(documento(), snapshot);

        assertThat(resposta.plan()).isNotNull();
        assertThat(resposta.plan().title()).isEqualTo("Plano de login");
        assertThat(resposta.plan().strategy()).isEqualTo("Cobrir 200/401/400");
        assertThat(resposta.plan().fileActions()).singleElement().satisfies(a -> {
            assertThat(a.relativePath()).isEqualTo("tests/api/auth/login.spec.ts");
            assertThat(a.operation()).isEqualTo("CREATE");
            assertThat(a.reason()).isEqualTo("cobrir autenticação");
        });
    }

    @Test
    void deveExporAsAdvertenciasDaAuditoriaJuntoComOPlano() {
        // É por elas que o usuário decide aprovar ou revisar.
        var snapshot = new AutoQaExecutionSnapshot();
        snapshot.setTechnicalPlan(plano());

        var resposta = mapper.toResponse(documento(), snapshot);

        assertThat(resposta.plan().warnings()).singleElement().satisfies(w -> {
            assertThat(w.code()).isEqualTo("POSSIVEL_DUPLICACAO");
            assertThat(w.requiresHumanDecision()).isTrue();
        });
    }

    @Test
    void deveDevolverPlanoNuloAntesDoPlanejamento() {
        var snapshot = new AutoQaExecutionSnapshot();

        assertThat(mapper.toResponse(documento(), snapshot).plan()).isNull();
    }

    @Test
    void deveDevolverPlanoNuloQuandoNaoHaSnapshot() {
        assertThat(mapper.toResponse(documento(), null).plan()).isNull();
    }
}
