package com.br.criarcenariotestes.business.config;

import com.br.criarcenariotestes.business.autoqa.scenario.CenarioSalvoResolver;

import com.br.criarcenariotestes.business.autoqa.executionapi.mapper.AutoQaExecutionResponseMapper;
import com.br.criarcenariotestes.business.autoqa.executionapi.orchestrator.AutoQaExecutionOrchestrator;
import com.br.criarcenariotestes.business.autoqa.executionapi.service.AutoQaExecutionQueryService;
import com.br.criarcenariotestes.controller.AutoQaExecutionController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comportamento real do CORS global (Fase 13.1B) com as duas origens
 * legítimas conhecidas configuradas simultaneamente — comprovando fail-open
 * apenas para elas, nunca para terceiros, e que preflight/requisições sem
 * Origin continuam funcionando como esperado. CorsConfig/AppCorsProperties
 * precisam ser importados explicitamente: @WebMvcTest não detecta
 * @Configuration comuns fora do controller sob teste automaticamente.
 */
@WebMvcTest(controllers = AutoQaExecutionController.class)
@Import({CorsConfig.class, AppCorsProperties.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:4200,http://100.83.72.100:9999")
@DisplayName("CorsConfig - Origens explicitamente autorizadas (Fase 13.1B)")
class CorsConfigAllowedOriginsTest {

    private static final String ORIGEM_DEV = "http://localhost:4200";
    private static final String ORIGEM_PRODUCAO = "http://100.83.72.100:9999";
    private static final String ORIGEM_NAO_AUTORIZADA = "http://evil.example";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AutoQaExecutionOrchestrator orchestrator;

    @MockitoBean
    private AutoQaExecutionQueryService queryService;

    @MockitoBean
    private AutoQaExecutionResponseMapper mapper;

    @MockitoBean
    private CenarioSalvoResolver cenarioSalvoResolver;

    @Test
    @DisplayName("Origem de desenvolvimento (localhost:4200) deve receber autorização CORS")
    void origemDesenvolvimentoDeveReceberAutorizacao() throws Exception {
        mockMvc.perform(get("/api/auto-qa/executions").header(HttpHeaders.ORIGIN, ORIGEM_DEV))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DEV));
    }

    @Test
    @DisplayName("Origem de produção (100.83.72.100:9999) deve receber autorização CORS")
    void origemProducaoDeveReceberAutorizacao() throws Exception {
        mockMvc.perform(get("/api/auto-qa/executions").header(HttpHeaders.ORIGIN, ORIGEM_PRODUCAO))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PRODUCAO));
    }

    @Test
    @DisplayName("Origem arbitrária não configurada não deve receber autorização CORS")
    void origemArbitrariaNaoDeveReceberAutorizacao() throws Exception {
        mockMvc.perform(get("/api/auto-qa/executions").header(HttpHeaders.ORIGIN, ORIGEM_NAO_AUTORIZADA))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Requisição sem header Origin não deve ser quebrada por CORS (clientes não-browser)")
    void requisicaoSemOriginNaoDeveSerQuebrada() throws Exception {
        mockMvc.perform(get("/api/auto-qa/executions"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Preflight (OPTIONS) de origem permitida deve funcionar")
    void preflightDeOrigemPermitidaDeveFuncionar() throws Exception {
        mockMvc.perform(options("/api/auto-qa/executions")
                        .header(HttpHeaders.ORIGIN, ORIGEM_DEV)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DEV));
    }

    @Test
    @DisplayName("Preflight (OPTIONS) de origem bloqueada não deve receber autorização CORS")
    void preflightDeOrigemBloqueadaNaoDeveAutorizar() throws Exception {
        mockMvc.perform(options("/api/auto-qa/executions")
                        .header(HttpHeaders.ORIGIN, ORIGEM_NAO_AUTORIZADA)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
