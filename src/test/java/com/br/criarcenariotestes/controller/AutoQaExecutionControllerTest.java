package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaExecutionListResponse;
import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaExecutionResponse;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.*;
import com.br.criarcenariotestes.business.autoqa.executionapi.mapper.AutoQaExecutionResponseMapper;
import com.br.criarcenariotestes.business.autoqa.executionapi.model.AutoQaWorkflowStatus;
import com.br.criarcenariotestes.business.autoqa.executionapi.orchestrator.AutoQaExecutionOrchestrator;
import com.br.criarcenariotestes.business.autoqa.executionapi.persistence.AutoQaExecutionDocument;
import com.br.criarcenariotestes.business.autoqa.executionapi.service.AutoQaExecutionQueryService;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyOperation;
import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import com.br.criarcenariotestes.business.autoqa.scenario.CenarioSalvoResolver;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AutoQaExecutionController.class)
@DisplayName("AutoQaExecutionController - Testes de Integração (MockMvc)")
class AutoQaExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AutoQaExecutionOrchestrator orchestrator;

    @MockitoBean
    private AutoQaExecutionQueryService queryService;

    @MockitoBean
    private AutoQaExecutionResponseMapper mapper;

    @MockitoBean
    private CenarioSalvoResolver cenarioSalvoResolver;

    /**
     * O texto do cenário passou a ser resolvido antes do orchestrator (pode
     * vir de um id salvo ou do texto avulso). Os testes que não tratam da
     * origem só precisam que ele devolva algo.
     */

    private UUID executionId;
    private AutoQaExecutionDocument document;

    @BeforeEach
    void setUp() {
        executionId = UUID.randomUUID();
        document = AutoQaExecutionDocument.createNew(executionId, "cenário", "/projeto/sensivel", Instant.now());
        when(cenarioSalvoResolver.resolverTexto(any())).thenReturn("cenário");
        when(mapper.toResponse(any())).thenAnswer(inv -> {
            AutoQaExecutionDocument doc = inv.getArgument(0);
            return new AutoQaExecutionResponse(doc.getExecutionId(), doc.getScenarioSummary(), doc.getWorkflowStatus(),
                    doc.getCurrentStage(), doc.getLastStageStarted(), doc.getLastStageCompleted(), doc.getAttempt(),
                    doc.getProgress(), Set.of(), List.of(), List.of(), doc.getCreatedAt(), doc.getUpdatedAt(),
                    doc.getStartedAt(), doc.getFinishedAt(), doc.getCancelledAt(), doc.getCancellationReason(), doc.getAutomationFramework());
        });
    }

    @Test
    @DisplayName("POST /executions deve retornar 201 Created")
    void createDeveRetornar201() throws Exception {
        when(orchestrator.create("cenário", "/projeto", null, null)).thenReturn(document);

        mockMvc.perform(post("/api/auto-qa/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenario":"cenário","projectPath":"/projeto"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.executionId").value(executionId.toString()));
    }

    @Test
    @DisplayName("POST /executions deve retornar 400 quando scenario estiver em branco")
    void createDeveRetornar400ComScenarioEmBranco() throws Exception {
        // Sem scenario e sem cenarioId não há o que automatizar - a regra
        // agora vive no CenarioSalvoResolver, porque @NotBlank por campo não
        // expressa "um OU outro".
        when(cenarioSalvoResolver.resolverTexto(any()))
                .thenThrow(new IllegalArgumentException("Informe cenarioId (cenário já salvo) ou scenario (texto)."));

        mockMvc.perform(post("/api/auto-qa/executions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenario":"","projectPath":"/projeto"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /executions/{id} deve retornar 200 quando encontrado")
    void getDeveRetornar200() throws Exception {
        when(queryService.get(executionId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/auto-qa/executions/{id}", executionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionId").value(executionId.toString()));
    }

    @Test
    @DisplayName("GET /executions/{id} deve retornar 404 quando não encontrado")
    void getDeveRetornar404() throws Exception {
        when(queryService.get(executionId)).thenThrow(new AutoQaExecutionNotFoundException("não encontrada"));

        mockMvc.perform(get("/api/auto-qa/executions/{id}", executionId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /executions deve retornar 200 com lista paginada")
    void listDeveRetornar200() throws Exception {
        when(queryService.list(any())).thenReturn(new AutoQaExecutionListResponse(List.of(sampleResponse()), 0, 20, 1));

        mockMvc.perform(get("/api/auto-qa/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /start deve retornar 202 Accepted")
    void startDeveRetornar202() throws Exception {
        when(orchestrator.start(executionId)).thenReturn(document);

        mockMvc.perform(post("/api/auto-qa/executions/{id}/start", executionId))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("POST /start deve retornar 409 em transição inválida")
    void startDeveRetornar409EmTransicaoInvalida() throws Exception {
        when(orchestrator.start(executionId)).thenThrow(new AutoQaInvalidTransitionException("inválido"));

        mockMvc.perform(post("/api/auto-qa/executions/{id}/start", executionId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /continue deve retornar 202 Accepted")
    void continueDeveRetornar202() throws Exception {
        when(orchestrator.continueExecution(executionId)).thenReturn(document);

        mockMvc.perform(post("/api/auto-qa/executions/{id}/continue", executionId))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("POST /generate deve retornar 202 Accepted")
    void generateDeveRetornar202() throws Exception {
        when(orchestrator.generate(executionId)).thenReturn(document);

        mockMvc.perform(post("/api/auto-qa/executions/{id}/generate", executionId))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("POST /apply-approval deve retornar 200 e chamar o orchestrator com o domínio correto")
    void applyApprovalDeveRetornar200() throws Exception {
        when(orchestrator.registerApplyApproval(org.mockito.ArgumentMatchers.eq(executionId), any())).thenReturn(document);

        mockMvc.perform(post("/api/auto-qa/executions/{id}/apply-approval", executionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approvedBy":"qa.lead","authorizedOperations":["CREATE"],"allowFileUpdate":true,"allowWarnings":true}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /apply deve retornar 403 quando ação sensível estiver desabilitada")
    void applyDeveRetornar403QuandoDesabilitada() throws Exception {
        when(orchestrator.apply(executionId)).thenThrow(new AutoQaSensitiveActionDisabledException("desabilitado"));

        mockMvc.perform(post("/api/auto-qa/executions/{id}/apply", executionId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /apply deve retornar 202 quando habilitado")
    void applyDeveRetornar202() throws Exception {
        when(orchestrator.apply(executionId)).thenReturn(document);

        mockMvc.perform(post("/api/auto-qa/executions/{id}/apply", executionId))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("POST /execution-approval deve retornar 200")
    void executionApprovalDeveRetornar200() throws Exception {
        when(orchestrator.registerExecutionApproval(org.mockito.ArgumentMatchers.eq(executionId), any())).thenReturn(document);

        mockMvc.perform(post("/api/auto-qa/executions/{id}/execution-approval", executionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"approvedBy":"qa.lead","allowedCommands":["PLAYWRIGHT_TEST"],"allowTestExecution":true,"allowInstallCommand":false,"allowBuildCommand":false}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /execute deve retornar 403 quando ação sensível estiver desabilitada")
    void executeDeveRetornar403QuandoDesabilitada() throws Exception {
        when(orchestrator.execute(executionId)).thenThrow(new AutoQaSensitiveActionDisabledException("desabilitado"));

        mockMvc.perform(post("/api/auto-qa/executions/{id}/execute", executionId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /execute deve retornar 202 quando habilitado")
    void executeDeveRetornar202() throws Exception {
        when(orchestrator.execute(executionId)).thenReturn(document);

        mockMvc.perform(post("/api/auto-qa/executions/{id}/execute", executionId))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("POST /cancel deve retornar 200")
    void cancelDeveRetornar200() throws Exception {
        when(orchestrator.cancel(org.mockito.ArgumentMatchers.eq(executionId), any())).thenReturn(document);

        mockMvc.perform(post("/api/auto-qa/executions/{id}/cancel", executionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"motivo\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /cancel sem corpo deve retornar 200 (reason opcional)")
    void cancelSemCorpoDeveRetornar200() throws Exception {
        when(orchestrator.cancel(org.mockito.ArgumentMatchers.eq(executionId), any())).thenReturn(document);

        mockMvc.perform(post("/api/auto-qa/executions/{id}/cancel", executionId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /cancel deve retornar 409 em execução já terminal")
    void cancelDeveRetornar409EmExecucaoTerminal() throws Exception {
        when(orchestrator.cancel(org.mockito.ArgumentMatchers.eq(executionId), any()))
                .thenThrow(new AutoQaExecutionConflictException("terminal"));

        mockMvc.perform(post("/api/auto-qa/executions/{id}/cancel", executionId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Optimistic locking deve retornar 409")
    void optimisticLockingDeveRetornar409() throws Exception {
        when(orchestrator.start(executionId)).thenThrow(new AutoQaOptimisticLockException("conflito de versão"));

        mockMvc.perform(post("/api/auto-qa/executions/{id}/start", executionId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Erro de reidratação (snapshot) deve retornar 422")
    void erroDeReidratacaoDeveRetornar422() throws Exception {
        when(orchestrator.start(executionId)).thenThrow(new AutoQaSnapshotException("inconsistente"));

        mockMvc.perform(post("/api/auto-qa/executions/{id}/start", executionId))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Resposta de erro nunca deve conter stacktrace nem nome de classe interna")
    void respostaDeErroNuncaContemStacktrace() throws Exception {
        when(orchestrator.start(executionId)).thenThrow(new AutoQaInvalidTransitionException("start só é permitido a partir de CREATED"));

        mockMvc.perform(post("/api/auto-qa/executions/{id}/start", executionId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("start só é permitido a partir de CREATED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Exception"))));
    }

    private AutoQaExecutionResponse sampleResponse() {
        return new AutoQaExecutionResponse(executionId, "cenário", AutoQaWorkflowStatus.CREATED, null, null, null,
                0, 0, Set.of(), List.of(), List.of(), Instant.now(), Instant.now(), null, null, null, null, null);
    }
}
