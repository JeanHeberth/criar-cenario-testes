package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.dto.CenarioResponse;
import com.br.criarcenariotestes.business.integration.OpenAiClient;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CenarioService {

    private final CenarioRepository cenarioRepository;
    private final OpenAiClient openAIClient;

    public CenarioResponse gerarCenarioCompleto(CenarioRequest request) {
        String resposta;

        try {
            resposta = openAIClient.gerarCenariosIA(
                    request.titulo(),
                    request.regraDeNegocio()
            );
        } catch (Exception e) {
            System.err.println("⚠️ Fallback acionado: " + e.getMessage());
            return gerarFallback(request);
        }

        // Parse da resposta
        String criterios = extrairSecao(resposta, "Critérios de Aceitação:", "Cenários de Teste:");
        List<String> cenariosList = extrairBlocos(resposta, "Cenário");

        Cenario cenario = new Cenario();
        cenario.setTitulo(request.titulo());
        cenario.setRegraDeNegocio(request.regraDeNegocio());
        cenario.setCriteriosAceitacao(criterios);
        cenario.setCenarios(cenariosList);

        Cenario salvo = cenarioRepository.save(cenario);

        return new CenarioResponse(
                salvo.getId(),
                salvo.getTitulo(),
                salvo.getRegraDeNegocio(),
                salvo.getCriteriosAceitacao(),
                salvo.getCenarios()
        );
    }


    private String extrairSecao(String texto, String inicio, String fim) {
        int idxInicio = texto.indexOf(inicio);
        int idxFim = texto.indexOf(fim);

        if (idxInicio != -1 && idxFim != -1 && idxInicio < idxFim) {
            return texto.substring(idxInicio + inicio.length(), idxFim).trim();
        }
        return "";
    }

    private List<String> extrairBlocos(String texto, String prefixo) {
        String[] linhas = texto.split("\\n");
        List<String> blocos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();

        for (String linha : linhas) {
            if (linha.trim().toLowerCase().startsWith(prefixo.toLowerCase())) {
                if (!atual.isEmpty()) {
                    blocos.add(atual.toString().trim());
                    atual = new StringBuilder();
                }
            }
            atual.append(linha).append("\n");
        }

        if (!atual.isEmpty()) {
            blocos.add(atual.toString().trim());
        }

        // remove blocos que contenham "Critérios de Aceitação:"
        return blocos.stream()
                .filter(b -> !b.toLowerCase().contains("critério de aceitação"))
                .toList();
    }



    private List<String> montarCenariosFallback(String titulo, String regra) {
        return List.of(
                String.format("Dado que %s, Quando %s, Então o sistema deve validar corretamente.", titulo, regra),
                String.format("Dado que o usuário está %s, Quando ele realiza %s, Então deve ocorrer a validação.", titulo, regra),
                String.format("Dado que uma pré-condição é %s, Quando %s acontece, Então o sistema deve reagir conforme a regra.", titulo, regra)
        );
    }

    private CenarioResponse gerarFallback(CenarioRequest request) {
        List<String> cenarios = montarCenariosFallback(request.titulo(), request.regraDeNegocio());

        return new CenarioResponse(
                null,
                request.titulo(),
                request.regraDeNegocio(),
                "CA1: O sistema deve funcionar mesmo sem resposta da IA.",
                cenarios
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
