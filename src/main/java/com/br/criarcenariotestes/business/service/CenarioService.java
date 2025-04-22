package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.dto.CenarioResponse;
import com.br.criarcenariotestes.business.integration.OpenAiClient;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CenarioService {

    private final CenarioRepository cenarioRepository;
    private final OpenAiClient openAIClient;


    public CenarioResponse gerarCenario(CenarioRequest request) {
        String cenarioGerado;

        try {
            cenarioGerado = openAIClient.gerarCenarioIA(
                    request.titulo(),
                    request.regraDeNegocio()
            );
        } catch (Exception e) {
            System.err.println("⚠️ Fallback acionado: " + e.getMessage());
            cenarioGerado = montarTextoCenario(request.titulo(), request.regraDeNegocio());
        }

        Cenario cenario = new Cenario();
        cenario.setTitulo(request.titulo());
        cenario.setRegraDeNegocio(request.regraDeNegocio());
        cenario.setCenarioGerado(cenarioGerado);

        Cenario salvo = cenarioRepository.save(cenario);

        return new CenarioResponse(
                salvo.getId(),
                salvo.getTitulo(),
                salvo.getRegraDeNegocio(),
                salvo.getCenarioGerado()
        );
    }

    private String montarTextoCenario(String titulo, String regra) {
        return String.format("""
                Dado que %s
                
                Quando %s
                
                Então o sistema deve seguir conforme esperado.
                """, titulo, regra);
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
