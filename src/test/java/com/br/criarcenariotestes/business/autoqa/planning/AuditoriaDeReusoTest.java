package com.br.criarcenariotestes.business.autoqa.planning;

import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import com.br.criarcenariotestes.business.autoqa.model.planning.ApprovalRequirement;
import com.br.criarcenariotestes.business.autoqa.model.planning.FileOperation;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlannedFileAction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec: para cada ação planejada como CREATE, cruzar com os componentes que o
 * PROJECT_KNOWLEDGE encontrou e emitir um veredito justificado.
 *
 * <p>O pipeline reutiliza arquivos hoje, mas nunca AUDITA a decisão de criar —
 * não há registro de por que criar era necessário. Duplicação passa silenciosa.
 */
class AuditoriaDeReusoTest {

    private final AuditoriaDeReuso auditoria = new AuditoriaDeReuso();

    private PlannedFileAction criar(String caminho) {
        return new PlannedFileAction(caminho, FileOperation.CREATE, PlanComponentType.API_CLIENT,
                "criar cliente", false, true, ApprovalRequirement.NONE, List.of(), List.of());
    }

    private ProjectComponent componente(String caminho, String nome) {
        return new ProjectComponent(caminho, nome, ComponentType.API_CLIENT, SourceLanguage.TYPESCRIPT,
                null, List.of(), List.of(), List.of(), List.of(), List.of(), false, true, List.of());
    }

    @Test
    void naoDeveAuditarCaminhoJaExistente() {
        // O PlanningValidator já reprova CREATE sobre arquivo existente, e de
        // forma mais estrita: lança exceção. Repetir a regra aqui produziria um
        // aviso que a validação nunca deixa chegar ao usuário.
        var vereditos = auditoria.auditar(
                List.of(criar("tests/api/auth/authApiClient.ts")),
                List.of(componente("tests/api/auth/authApiClient.ts", "authApiClient")));

        assertThat(vereditos).singleElement()
                .satisfies(v -> assertThat(v.tipo()).isEqualTo(AuditoriaDeReuso.Tipo.CRIAR));
    }

    @Test
    void deveAcusarEstenderQuandoExisteMesmoNomeEmOutroCaminho() {
        var vereditos = auditoria.auditar(
                List.of(criar("tests/api/auth/authApiClient.ts")),
                List.of(componente("tests/api/shared/authApiClient.ts", "authApiClient")));

        assertThat(vereditos).singleElement().satisfies(v -> {
            assertThat(v.tipo()).isEqualTo(AuditoriaDeReuso.Tipo.ESTENDER);
            assertThat(v.localizacao()).isEqualTo("tests/api/shared/authApiClient.ts");
        });
    }

    @Test
    void deveAprovarCriacaoQuandoNaoHaCorrespondente() {
        var vereditos = auditoria.auditar(
                List.of(criar("tests/api/auth/authApiClient.ts")),
                List.of(componente("tests/api/pedidos/pedidoClient.ts", "pedidoClient")));

        assertThat(vereditos).singleElement()
                .satisfies(v -> assertThat(v.tipo()).isEqualTo(AuditoriaDeReuso.Tipo.CRIAR));
    }

    @Test
    void naoDeveAuditarAcoesQueJaNaoSaoCriacao() {
        // REUSE e UPDATE já declaram a intenção; a auditoria existe para
        // questionar a decisão de CRIAR.
        var reuse = new PlannedFileAction("tests/api/auth/x.ts", FileOperation.REUSE,
                PlanComponentType.API_CLIENT, "reusar", true, true,
                ApprovalRequirement.NONE, List.of(), List.of());

        assertThat(auditoria.auditar(List.of(reuse), List.of())).isEmpty();
    }

    @Test
    void deveConverterVereditosEmAdvertenciasDoPlano() {
        // O veredito só tem valor se aparecer para quem decide. CRIAR não vira
        // advertência: criação justificada é o caso normal e viraria ruído.
        var vereditos = List.of(
                new AuditoriaDeReuso.Veredito("b.ts", AuditoriaDeReuso.Tipo.ESTENDER, "duplicado", "outro/b.ts"),
                new AuditoriaDeReuso.Veredito("c.ts", AuditoriaDeReuso.Tipo.CRIAR, "sem correspondente", null));

        var advertencias = auditoria.comoAdvertencias(vereditos);

        assertThat(advertencias).singleElement().satisfies(a -> {
            assertThat(a.code()).isEqualTo("POSSIVEL_DUPLICACAO");
            assertThat(a.description()).contains("b.ts").contains("ESTENDER").contains("outro/b.ts");
            assertThat(a.requiresHumanDecision())
                    .as("só quem conhece o domínio sabe se é duplicação ou separação legítima")
                    .isTrue();
        });
    }
}
