package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.dto.CenarioResponse;
import com.br.criarcenariotestes.business.integration.OpenAiClient;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CenarioService {

    private final CenarioRepository cenarioRepository;
    private final OpenAiClient openAIClient;

    public CenarioResponse gerarCenarioUnificado(CenarioRequest request) {
        List<String> cenariosGerados;

        try {
            cenariosGerados = openAIClient.gerarCenariosIA(
                    request.titulo(),
                    request.regraDeNegocio()
            );
        } catch (Exception e) {
            System.err.println("⚠️ Fallback acionado: " + e.getMessage());
            cenariosGerados = montarCenariosFallback(request.titulo(), request.regraDeNegocio());
        }

        // Junta todos os cenários em um único texto com separador visível
        String cenarioCompleto = cenariosGerados.stream()
                .map(String::trim)
                .collect(Collectors.joining("\n\n"));

        Cenario cenario = new Cenario();
        cenario.setTitulo(request.titulo());
        cenario.setRegraDeNegocio(request.regraDeNegocio());
        cenario.setCenarioGerado(cenarioCompleto);

        Cenario salvo = cenarioRepository.save(cenario);

        return new CenarioResponse(
                salvo.getId(),
                salvo.getTitulo(),
                salvo.getRegraDeNegocio(),
                salvo.getCenarioGerado()
        );
    }


    private List<String> montarCenariosFallback(String titulo, String regra) {
        return List.of(
                String.format("Dado que %s, Quando %s, Então o sistema deve validar corretamente.", titulo, regra),
                String.format("Dado que o usuário está %s, Quando ele realiza %s, Então deve ocorrer a validação.", titulo, regra),
                String.format("Dado que uma pré-condição é %s, Quando %s acontece, Então o sistema deve reagir conforme a regra.", titulo, regra)
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
}
