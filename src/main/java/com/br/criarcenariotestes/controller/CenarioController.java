package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.dto.CenarioResponse;
import com.br.criarcenariotestes.business.service.CenarioService;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://192.168.1.99:4200",
        originPatterns = "http://localhost:4200")
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
    public Cenario buscarCenario(String id) {
        return cenarioService.buscarCenario(id);
    }

    @DeleteMapping("/{id}")
    public void excluirCenario(String id) {
        cenarioService.excluirCenario(id);
    }
}
