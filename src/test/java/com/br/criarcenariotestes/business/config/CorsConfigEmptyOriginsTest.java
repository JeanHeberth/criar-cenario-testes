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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * app.cors.allowed-origins vazia (default, equivalente a nenhuma variável de
 * ambiente configurada) deve ser fail-closed: nenhuma origem — nem mesmo
 * localhost — recebe autorização CORS. Nunca equivalente a "*".
 */
@WebMvcTest(controllers = AutoQaExecutionController.class)
@Import({CorsConfig.class, AppCorsProperties.class})
@TestPropertySource(properties = "app.cors.allowed-origins=")
@DisplayName("CorsConfig - Configuração vazia é fail-closed (Fase 13.1B)")
class CorsConfigEmptyOriginsTest {

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
    @DisplayName("Nenhuma origem, nem mesmo localhost, deve receber autorização CORS quando a lista está vazia")
    void nenhumaOrigemDeveSerAutorizadaComListaVazia() throws Exception {
        mockMvc.perform(get("/api/auto-qa/executions").header(HttpHeaders.ORIGIN, "http://localhost:4200"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Requisição sem Origin continua funcionando normalmente mesmo com allowed-origins vazia")
    void requisicaoSemOriginContinuaFuncionandoComListaVazia() throws Exception {
        mockMvc.perform(get("/api/auto-qa/executions"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
