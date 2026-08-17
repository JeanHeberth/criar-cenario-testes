package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.dto.CenarioResponse;
import com.br.criarcenariotestes.business.dto.DestinoPublicacaoResponse;
import com.br.criarcenariotestes.business.dto.WorkflowInfoResponse;
import com.br.criarcenariotestes.business.service.CenarioService;
import com.br.criarcenariotestes.business.service.DestinoPublicacaoService;
import com.br.criarcenariotestes.business.workflow.WorkflowType;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/cenario")
@RequiredArgsConstructor
public class CenarioController {


    private final CenarioService cenarioService;
    private final DestinoPublicacaoService destinoPublicacaoService;

    @PostMapping
    public CenarioResponse gerarCenario(@RequestBody CenarioRequest cenarioRequest) {
        return cenarioService.gerarCenarioCompleto(cenarioRequest);
    }
    
    /**
     * Onde a publicação vai parar, resolvido sem gerar nada. A tela usa isto
     * para mostrar o destino (e validar a referência da tarefa) antes de o
     * usuário disparar uma geração — que custa chamadas de IA e, se publicar
     * no lugar errado, deixa pasta permanente no Zephyr.
     */
    @GetMapping("/destino")
    public DestinoPublicacaoResponse previewDestino(
            @RequestParam(required = false) String taskRef,
            @RequestParam(required = false) String pastaDestino,
            @RequestParam(required = false) String projectKey) {
        return destinoPublicacaoService.resolver(taskRef, pastaDestino, projectKey);
    }

    @GetMapping("/workflows")
    public List<WorkflowInfoResponse> listarWorkflows() {
        return Arrays.stream(WorkflowType.values())
                .map(WorkflowInfoResponse::from)
                .toList();
    }

    @GetMapping
    public List<Cenario> listarCenarios() {
        return cenarioService.listarCenarios();
    }

    @GetMapping("/{id}")
    public Cenario buscarCenario(@PathVariable String id) {
        return cenarioService.buscarCenario(id);
    }

    @DeleteMapping("/{id}")
    public void excluirCenario(@PathVariable String id) {
        cenarioService.excluirCenario(id);
    }

    @PostMapping(value = "/com-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CenarioResponse gerarComPdf(
            @RequestParam String titulo,
            @RequestParam String regraDeNegocio,
            @RequestParam(required = false) String agent,
            @RequestParam(required = false) String taskRef,
            @RequestParam("arquivos") List<MultipartFile> arquivos
    ) {
        return cenarioService.gerarCenarioComPdf(titulo, regraDeNegocio, agent, taskRef, arquivos);
    }
}
