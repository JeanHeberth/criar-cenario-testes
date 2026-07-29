package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ZephyrFormatterAgent - Testes Unitários")
class ZephyrFormatterAgentTest {

    private ZephyrFormatterAgent agent;
    private WorkflowContext context;

    @BeforeEach
    void setUp() {
        agent = new ZephyrFormatterAgent();
        
        CenarioRequest request = new CenarioRequest(
                "Login OAuth",
                "Sistema de login com OAuth",
                "gerador_cenarios_testes"
        );
        context = new WorkflowContext(request);
        context.setCriteriosAceitacao("Sistema deve permitir login via OAuth");

        CenarioItem cenario1 = new CenarioItem();
        cenario1.setNome("Login com credenciais válidas");
        cenario1.setObjetivo("Validar login bem-sucedido");
        cenario1.setPrecondicao("Usuário cadastrado");
        cenario1.setScriptTeste("1. Acessar tela\n2. Informar credenciais\n3. Clicar em entrar");
        cenario1.setResultadoEsperado("Login realizado com sucesso");

        CenarioItem cenario2 = new CenarioItem();
        cenario2.setNome("Login com senha inválida");
        cenario2.setObjetivo("Validar erro de senha");
        cenario2.setScriptTeste("1. Informar senha errada");
        cenario2.setResultadoEsperado("Exibir mensagem de erro");

        context.setCenariosRevisados(List.of(cenario1, cenario2));
    }

    @Test
    @DisplayName("Deve formatar cenários para padrão Zephyr")
    void deveFormatarParaZephyr() {
        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getFormatoFinal()).isNotNull();
        assertThat(context.getFormatoFinal()).contains("# Casos de Teste - Login OAuth");
        assertThat(context.getFormatoFinal()).contains("### CT001 - Login com credenciais válidas");
        assertThat(context.getFormatoFinal()).contains("### CT002 - Login com senha inválida");
        assertThat(context.getFormatoFinal()).contains("**Objetivo:**");
        assertThat(context.getFormatoFinal()).contains("**Passos:**");
        assertThat(context.getFormatoFinal()).contains("**Resultado Esperado:**");
        assertThat(context.getMetadata("formato")).isEqualTo("Zephyr");
    }

    @Test
    @DisplayName("Deve retornar nome correto do agente")
    void deveRetornarNomeCorreto() {
        // Assert
        assertThat(agent.getNome()).isEqualTo("Zephyr Formatter");
    }

    @Test
    @DisplayName("Deve usar cenários originais se revisados não existirem")
    void deveUsarCenariosOriginaisSeRevisadosNaoExistirem() {
        // Arrange
        CenarioItem cenario = new CenarioItem();
        cenario.setNome("CT Original");
        context.setCenariosRevisados(null);
        context.setCenarios(List.of(cenario));

        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getFormatoFinal()).contains("CT001 - CT Original");
    }

    @Test
    @DisplayName("Deve incluir critérios de aceitação quando disponível")
    void deveIncluirCriteriosDeAceitacao() {
        // Act
        agent.executar(context);

        // Assert
        assertThat(context.getFormatoFinal()).contains("**Critérios de Aceitação:**");
        assertThat(context.getFormatoFinal()).contains("Sistema deve permitir login via OAuth");
    }

    @Test
    @DisplayName("Deve lidar com cenários vazios gracefully")
    void deveLidarComCenariosVazios() {
        // Arrange
        context.setCenariosRevisados(List.of());
        context.setCenarios(List.of());

        // Act
        agent.executar(context);

        // Assert - não deve lançar exceção
        assertThat(context.getFormatoFinal()).isNull();
    }

    @Test
    @DisplayName("Deve formatar passos corretamente")
    void deveFormatarPassosCorretamente() {
        // Act
        agent.executar(context);

        // Assert
        String formatoFinal = context.getFormatoFinal();
        assertThat(formatoFinal).contains("1. Acessar tela");
        assertThat(formatoFinal).contains("2. Informar credenciais");
        assertThat(formatoFinal).contains("3. Clicar em entrar");
    }
}
