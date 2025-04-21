package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.dto.CenarioResponse;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CenarioService {

    private final CenarioRepository cenarioRepository;


    public CenarioResponse gerarCenario(CenarioRequest cenarioRequest) {

        // Gera o texto do cenário com função isolada
        String cenarioGerado = montarTextoCenario(
                cenarioRequest.titulo(),
                cenarioRequest.regraDeNegocio()
        );

        // Cria entidade
        Cenario cenario = new Cenario();
        cenario.setTitulo(cenarioRequest.titulo());
        cenario.setRegraDeNegocio(cenarioRequest.regraDeNegocio());
        cenario.setCenarioGerado(cenarioGerado);

        // Salva no MongoDB
        Cenario salvo = cenarioRepository.save(cenario);

        // Retorna a resposta formatada
        return new CenarioResponse(
                salvo.getId(),
                salvo.getTitulo(),
                salvo.getRegraDeNegocio(),
                salvo.getCenarioGerado()
        );
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

    private String montarTextoCenario(String titulo, String regraDeNegocio) {
        return String.format("""
        Dado que %s.

        Quando %s.

        Então o sistema deve seguir conforme esperado.
        """, titulo, regraDeNegocio
        );
    }
}
