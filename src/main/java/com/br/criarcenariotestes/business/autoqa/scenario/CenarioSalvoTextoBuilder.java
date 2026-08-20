package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converte um cenário já salvo (o que aparece na "Lista de Cenários de Teste")
 * no texto que o pipeline do Auto QA consome.
 *
 * Existe para fechar o elo entre as duas metades do produto: os cenários eram
 * gerados e persistidos de um lado, e o Auto QA só aceitava texto colado do
 * outro. Aqui eles passam a alimentar a automação diretamente.
 *
 * Manda o documento INTEIRO num texto só, não um item por execução. Além de
 * custar uma rodada de chamadas de IA em vez de N, o agente enxerga a regra de
 * negócio e os critérios de aceitação junto dos casos — contexto que um
 * cenário solto não carrega, e que muda o que ele consegue gerar.
 */
@Component
public class CenarioSalvoTextoBuilder {

    public String construir(Cenario cenario) {
        StringBuilder texto = new StringBuilder();

        // Cabeçalho só quando há título: escrevê-lo vazio faria um cenário sem
        // conteúdo algum parecer preenchido, e o resolver mandaria "Título:"
        // sozinho para o agente em vez de recusar.
        if (temTexto(cenario.getTitulo())) {
            texto.append("Título: ").append(cenario.getTitulo().trim()).append("\n\n");
        }

        if (temTexto(cenario.getRegraDeNegocio())) {
            texto.append("Regra de negócio:\n").append(cenario.getRegraDeNegocio().trim()).append("\n\n");
        }

        if (temTexto(cenario.getCriteriosAceitacao())) {
            texto.append("Critérios de aceitação:\n").append(cenario.getCriteriosAceitacao().trim()).append("\n\n");
        }

        List<CenarioItem> itens = cenario.getCenarios();
        if (itens == null || itens.isEmpty()) {
            return texto.toString().trim();
        }

        texto.append("Cenários de teste a automatizar (").append(itens.size()).append("):\n");

        int numero = 1;
        for (CenarioItem item : itens) {
            texto.append("\n--- Cenário ").append(numero++).append(" ---\n");
            texto.append("Nome: ").append(valorOuVazio(item.getNome())).append("\n");

            if (temTexto(item.getObjetivo())) {
                texto.append("Objetivo: ").append(item.getObjetivo().trim()).append("\n");
            }
            if (temTexto(item.getPrecondicao())) {
                texto.append("Pré-condições: ").append(item.getPrecondicao().trim()).append("\n");
            }
            if (temTexto(item.getScriptTeste())) {
                texto.append("Passos:\n").append(item.getScriptTeste().trim()).append("\n");
            }
            if (temTexto(item.getResultadoEsperado())) {
                texto.append("Resultado esperado: ").append(item.getResultadoEsperado().trim()).append("\n");
            }
            // A key do Zephyr entra como rastreabilidade: permite ao agente
            // referenciá-la no teste gerado, ligando código e caso de teste.
            if (temTexto(item.getZephyrTestCaseKey())) {
                texto.append("Caso de teste no Zephyr: ").append(item.getZephyrTestCaseKey().trim()).append("\n");
            }
        }

        return texto.toString().trim();
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String valorOuVazio(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
