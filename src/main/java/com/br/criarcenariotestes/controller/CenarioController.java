package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.dto.CenarioResponse;
import com.br.criarcenariotestes.business.service.CenarioService;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/cenario")
@RequiredArgsConstructor
public class CenarioController {


    private final CenarioService cenarioService;

    @PostMapping
    public CenarioResponse gerarCenario(@RequestBody CenarioRequest cenarioRequest) {
        return cenarioService.gerarCenarioCompleto(cenarioRequest);
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
            @RequestParam("arquivos") List<MultipartFile> arquivos
    ) {
        return cenarioService.gerarCenarioComPdf(titulo, regraDeNegocio, arquivos);
    }
}
