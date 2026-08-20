package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.autoqa.executionapi.dto.AutoQaCreateExecutionRequest;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Decide o texto do cenário que a execução do Auto QA vai usar: carrega o
 * cenário salvo quando veio um id, ou usa o texto informado direto.
 */
@Service
@RequiredArgsConstructor
public class CenarioSalvoResolver {

    private static final Logger log = LoggerFactory.getLogger(CenarioSalvoResolver.class);

    private final CenarioRepository cenarioRepository;
    private final CenarioSalvoTextoBuilder textoBuilder;

    public String resolverTexto(AutoQaCreateExecutionRequest request) {
        if (request.temCenarioId() && request.temScenario()) {
            throw new IllegalArgumentException(
                    "Informe cenarioId ou scenario, não os dois - não há como saber qual deveria valer.");
        }

        if (request.temScenario()) {
            return request.scenario();
        }

        if (!request.temCenarioId()) {
            throw new IllegalArgumentException("Informe cenarioId (cenário já salvo) ou scenario (texto).");
        }

        Cenario cenario = cenarioRepository.findById(request.cenarioId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cenário não encontrado: " + request.cenarioId()));

        String texto = textoBuilder.construir(cenario);
        if (texto.isBlank()) {
            throw new IllegalArgumentException(
                    "Cenário '" + request.cenarioId() + "' não tem conteúdo para automatizar.");
        }

        log.info("Auto QA usando cenário salvo. id='{}', titulo='{}', itens={}, tamanhoTexto={}",
                cenario.getId(),
                cenario.getTitulo(),
                cenario.getCenarios() == null ? 0 : cenario.getCenarios().size(),
                texto.length());

        return texto;
    }
}
