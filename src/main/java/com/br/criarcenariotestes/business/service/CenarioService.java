package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CenarioService {

    private final CenarioRepository cenarioRepository;


    public Cenario gerarCenario(Cenario cenario) {
        String cenarioTeste = String.format("Dado que %s\n Quando %s\n Então o sistema deve seguir conforme esperado",
                cenario.getTitulo(),
                cenario.getRegraDeNegocio());

        cenario.setCenarioGerado(cenarioTeste);
        return cenarioRepository.save(cenario);
    }

    public List<Cenario> listarCenarios() {
        return cenarioRepository.findAll();
    }

    public Cenario buscarCenario(String id) {
        return cenarioRepository.findById(id).orElse(null);
    }

    public void excluirCenario(String id) {
        cenarioRepository.deleteById(id);
    }
}
