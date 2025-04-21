package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.service.CenarioService;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cenario")
@RequiredArgsConstructor
public class CenarioController {

    private final CenarioService cenarioService;

    @PostMapping
    public Cenario gerarCenario(@RequestBody Cenario cenario) {
        return cenarioService.gerarCenario(cenario);
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
