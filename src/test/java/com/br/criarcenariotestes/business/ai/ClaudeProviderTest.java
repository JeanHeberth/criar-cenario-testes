package com.br.criarcenariotestes.business.ai;

import com.br.criarcenariotestes.business.properties.ClaudeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClaudeProviderTest {

    private ClaudeProperties properties;
    private ClaudeProvider provider;

    @BeforeEach
    void setUp() {
        properties = new ClaudeProperties();
        properties.setApiKey("sk-ant-teste");
        provider = new ClaudeProvider(properties);
    }

    @Test
    @DisplayName("Deve se identificar como 'claude' para o AiProviderResolver")
    void getName_deveRetornarClaude() {
        assertEquals("claude", provider.getName());
    }

    @Test
    @DisplayName("Deve falhar explicitamente quando ANTHROPIC_API_KEY não está configurada")
    void gerarResposta_semApiKey_deveFalharComMensagemClara() {
        properties.setApiKey("  ");

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> provider.gerarResposta("system", "user"));

        assertTrue(erro.getMessage().contains("ANTHROPIC_API_KEY"),
                "Mensagem deve nomear a variável faltante: " + erro.getMessage());
    }

    @Test
    @DisplayName("Deve falhar explicitamente quando o modelo não está configurado")
    void gerarResposta_semModelo_deveFalhar() {
        properties.setModel("");

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> provider.gerarResposta("system", "user"));

        assertTrue(erro.getMessage().contains("Modelo Claude"), erro.getMessage());
    }

    @Test
    @DisplayName("Com thinking ligado, o override do chamador ganha folga porque raciocínio e texto dividem o mesmo teto")
    void resolverMaxTokens_comThinking_deveSomarFolga() {
        properties.setThinkingEnabled(true);
        properties.setMaxTokens(16000);
        properties.setThinkingHeadroomTokens(8000);

        // TestScenarioAgent.GENERATOR_MAX_TOKENS = 8000 → 8000 + 8000 = 16000
        assertEquals(16000, provider.resolverMaxTokens(8000));
        // Override alto continua ganhando a folga
        assertEquals(28000, provider.resolverMaxTokens(20000));
    }

    @Test
    @DisplayName("Com thinking ligado, nunca deve ficar abaixo do teto configurado")
    void resolverMaxTokens_overrideBaixo_deveRespeitarPisoConfigurado() {
        properties.setThinkingEnabled(true);
        properties.setMaxTokens(16000);
        properties.setThinkingHeadroomTokens(8000);

        assertEquals(16000, provider.resolverMaxTokens(1000));
    }

    @Test
    @DisplayName("Com thinking desligado, o override do chamador é respeitado sem folga")
    void resolverMaxTokens_semThinking_deveUsarOverrideDireto() {
        properties.setThinkingEnabled(false);
        properties.setMaxTokens(16000);

        assertEquals(8000, provider.resolverMaxTokens(8000));
    }

    @Test
    @DisplayName("Sem override, deve usar o limite configurado")
    void resolverMaxTokens_semOverride_deveUsarConfigurado() {
        properties.setThinkingEnabled(false);
        properties.setMaxTokens(16000);

        assertEquals(16000, provider.resolverMaxTokens(null));
    }

    @Test
    @DisplayName("Deve normalizar o effort configurado")
    void resolverEffort_deveNormalizar() {
        properties.setEffort("  HIGH ");
        assertEquals("high", provider.resolverEffort());

        properties.setEffort(null);
        assertEquals("medium", provider.resolverEffort());

        properties.setEffort("");
        assertEquals("medium", provider.resolverEffort());
    }

    @Test
    @DisplayName("Deve rebaixar effort xhigh/max quando thinking está desligado, pois a API rejeita a combinação com HTTP 400")
    void resolverEffort_thinkingDesligado_deveRebaixarEffortIncompativel() {
        properties.setThinkingEnabled(false);

        properties.setEffort("xhigh");
        assertEquals("high", provider.resolverEffort());

        properties.setEffort("max");
        assertEquals("high", provider.resolverEffort());

        // Níveis compatíveis não são alterados
        properties.setEffort("medium");
        assertEquals("medium", provider.resolverEffort());
    }

    @Test
    @DisplayName("Com thinking ligado, effort xhigh/max é combinação válida e deve ser preservado")
    void resolverEffort_thinkingLigado_naoDeveRebaixar() {
        properties.setThinkingEnabled(true);
        properties.setEffort("max");

        assertEquals("max", provider.resolverEffort());
    }
}
