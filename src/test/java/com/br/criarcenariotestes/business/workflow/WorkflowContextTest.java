package com.br.criarcenariotestes.business.workflow;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkflowContext - Testes Unitários")
class WorkflowContextTest {

    private CenarioRequest request;

    @BeforeEach
    void setUp() {
        request = new CenarioRequest(
                "Login OAuth",
                "Sistema de login",
                "gerador_cenarios_testes"
        );
    }

    @Test
    @DisplayName("Deve criar context com workflow COMPLETO por padrão")
    void deveCriarContextComWorkflowCompletoPorPadrao() {
        // Act
        WorkflowContext context = new WorkflowContext(request);

        // Assert
        assertThat(context.getRequest()).isEqualTo(request);
        assertThat(context.getWorkflowType()).isEqualTo(WorkflowType.COMPLETO);
        assertThat(context.getMetadados()).isNotNull();
        assertThat(context.getMetadados()).isEmpty();
    }

    @Test
    @DisplayName("Deve criar context com workflow específico")
    void deveCriarContextComWorkflowEspecifico() {
        // Act
        WorkflowContext context = new WorkflowContext(request, WorkflowType.RAPIDO);

        // Assert
        assertThat(context.getWorkflowType()).isEqualTo(WorkflowType.RAPIDO);
    }

    @Test
    @DisplayName("Deve adicionar e recuperar metadados")
    void deveAdicionarERecuperarMetadados() {
        // Arrange
        WorkflowContext context = new WorkflowContext(request);

        // Act
        context.addMetadata("provider", "OpenAI");
        context.addMetadata("tokens", 1500);

        // Assert
        assertThat(context.getMetadata("provider")).isEqualTo("OpenAI");
        assertThat(context.getMetadata("tokens")).isEqualTo(1500);
        assertThat(context.getMetadata("inexistente")).isNull();
    }

    @Test
    @DisplayName("Deve retornar cenários finais - prioriza revisados")
    void deveRetornarCenariosFinaisPriorizaRevisados() {
        // Arrange
        WorkflowContext context = new WorkflowContext(request);

        CenarioItem cenarioOriginal = new CenarioItem();
        cenarioOriginal.setNome("Original");

        CenarioItem cenarioRevisado = new CenarioItem();
        cenarioRevisado.setNome("Revisado");

        context.setCenarios(List.of(cenarioOriginal));
        context.setCenariosRevisados(List.of(cenarioRevisado));

        // Act
        List<CenarioItem> finais = context.getCenariosFinais();

        // Assert
        assertThat(finais).hasSize(1);
        assertThat(finais.get(0).getNome()).isEqualTo("Revisado");
    }

    @Test
    @DisplayName("Deve retornar cenários originais se revisados vazios")
    void deveRetornarCenariosOriginaisSeRevisadosVazios() {
        // Arrange
        WorkflowContext context = new WorkflowContext(request);

        CenarioItem cenarioOriginal = new CenarioItem();
        cenarioOriginal.setNome("Original");

        context.setCenarios(List.of(cenarioOriginal));
        context.setCenariosRevisados(List.of());

        // Act
        List<CenarioItem> finais = context.getCenariosFinais();

        // Assert
        assertThat(finais).hasSize(1);
        assertThat(finais.get(0).getNome()).isEqualTo("Original");
    }

    @Test
    @DisplayName("Deve retornar cenários originais se revisados nulos")
    void deveRetornarCenariosOriginaisSeRevisadosNulos() {
        // Arrange
        WorkflowContext context = new WorkflowContext(request);

        CenarioItem cenarioOriginal = new CenarioItem();
        cenarioOriginal.setNome("Original");

        context.setCenarios(List.of(cenarioOriginal));
        context.setCenariosRevisados(null);

        // Act
        List<CenarioItem> finais = context.getCenariosFinais();

        // Assert
        assertThat(finais).hasSize(1);
        assertThat(finais.get(0).getNome()).isEqualTo("Original");
    }

    @Test
    @DisplayName("Deve permitir definir todos os campos")
    void devePermitirDefinirTodosOsCampos() {
        // Arrange
        WorkflowContext context = new WorkflowContext(request);

        // Act
        context.setAgentInstructions("Instruções customizadas");
        context.setRequisitos("RF001: Login");
        context.setDecisoesReuniao("Usar OAuth 2.0");
        context.setPlanoMacro("Testar cenários positivos e negativos");
        context.setCenarios(List.of(new CenarioItem()));
        context.setCriteriosAceitacao("Sistema deve permitir login");
        context.setFormatoFinal("# CT001");

        // Assert
        assertThat(context.getAgentInstructions()).isEqualTo("Instruções customizadas");
        assertThat(context.getRequisitos()).isEqualTo("RF001: Login");
        assertThat(context.getDecisoesReuniao()).isEqualTo("Usar OAuth 2.0");
        assertThat(context.getPlanoMacro()).isEqualTo("Testar cenários positivos e negativos");
        assertThat(context.getCenarios()).hasSize(1);
        assertThat(context.getCriteriosAceitacao()).isEqualTo("Sistema deve permitir login");
        assertThat(context.getFormatoFinal()).isEqualTo("# CT001");
    }
}
