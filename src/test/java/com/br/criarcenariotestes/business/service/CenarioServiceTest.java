package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.document.PdfTextExtractor;
import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.dto.CenarioResponse;
import com.br.criarcenariotestes.business.fallback.CenarioFallbackFactory;
import com.br.criarcenariotestes.business.parser.CenarioTextoParser;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CenarioServiceTest {

    @Mock
    private PdfTextExtractor pdfTextExtractor;
    @Mock
    private CenarioRepository cenarioRepository;
    @Mock
    private AiProviderResolver aiProviderResolver;
    @Mock
    private CenarioFallbackFactory fallbackFactory;
    @Mock
    private AgentLoaderService agentLoaderService;
    @Mock
    private AiProvider activeProvider;

    private CenarioService cenarioService;

    @BeforeEach
    void setUp() {
        cenarioService = new CenarioService(
                pdfTextExtractor,
                cenarioRepository,
                aiProviderResolver,
                new CenarioTextoParser(),
                fallbackFactory,
                agentLoaderService
        );
    }

    @Test
    void devePersistirTodosOsCenariosRetornadosPelaIaSemReduzirParaDois() {
        CenarioRequest request = new CenarioRequest("Pagamento", "Regra de negocio", null);

        when(aiProviderResolver.getActiveProvider()).thenReturn(activeProvider);
        when(activeProvider.gerarResposta(anyString(), anyString())).thenReturn(respostaIaComSeisCenarios());
        when(cenarioRepository.save(any(Cenario.class))).thenAnswer(invocation -> {
            Cenario salvo = invocation.getArgument(0);
            salvo.setId("cenario-123");
            return salvo;
        });

        CenarioResponse response = cenarioService.gerarCenarioCompleto(request);

        assertNotNull(response);
        assertEquals(6, response.cenarios().size());

        ArgumentCaptor<Cenario> captor = ArgumentCaptor.forClass(Cenario.class);
        verify(cenarioRepository).save(captor.capture());
        assertEquals(6, captor.getValue().getCenarios().size());
        verify(aiProviderResolver, never()).getFallbackProvider();
    }

    private String respostaIaComSeisCenarios() {
        StringBuilder sb = new StringBuilder();

        for (int i = 1; i <= 6; i++) {
            if (i > 1) {
                sb.append("\n---\n");
            }
            sb.append("Nome: Cenario ").append(i).append("\n")
                    .append("Objetivo: Validar fluxo ").append(i).append("\n")
                    .append("Precondição: Ambiente pronto\n")
                    .append("Script de Teste (Passo-a-Passo): Dado usuario autenticado\\nQuando realiza a acao\n")
                    .append("Script de Teste (Passo-a-Passo) - Resultado: Entao operacao concluida\n")
                    .append("Variáveis: Nao se aplica\n")
                    .append("Componente: Backend\n")
                    .append("Rótulos: regressao\n")
                    .append("Propósito: Cobertura funcional\n")
                    .append("Pasta: Funcionalidade > Pagamento\n")
                    .append("Proprietário: JIRAUSER23105\n")
                    .append("Cobertura: REG-").append(i).append("\n")
                    .append("Status: APPROVED\n");
        }

        return sb.toString();
    }
}

