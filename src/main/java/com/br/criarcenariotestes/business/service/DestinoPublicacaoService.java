package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.dto.DestinoPublicacaoResponse;
import com.br.criarcenariotestes.business.properties.ZephyrProperties;
import com.br.criarcenariotestes.business.tracker.FolderStrategyResolver;
import com.br.criarcenariotestes.business.tracker.ProvedorTarefa;
import com.br.criarcenariotestes.business.tracker.ReferenciaTarefa;
import com.br.criarcenariotestes.business.tracker.ReferenciaTarefaInvalidaException;
import com.br.criarcenariotestes.business.tracker.ReferenciaTarefaParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Resolve o destino da publicação sem gerar nada — é o que permite a tela
 * mostrar "vai publicar em X/Y" antes de o usuário disparar a geração.
 *
 * Aplica exatamente a mesma precedência do ZephyrPublisherAgent, de propósito:
 * um preview que divergisse do comportamento real seria pior que não ter
 * preview.
 */
@Service
@RequiredArgsConstructor
public class DestinoPublicacaoService {

    private final ReferenciaTarefaParser referenciaTarefaParser;
    private final FolderStrategyResolver folderStrategyResolver;
    private final ZephyrProperties zephyrProperties;

    public DestinoPublicacaoResponse resolver(String taskRef, String pastaDestino, String projectKey) {
        Optional<ReferenciaTarefa> referencia;
        try {
            referencia = referenciaTarefaParser.parsear(taskRef);
        } catch (ReferenciaTarefaInvalidaException e) {
            return DestinoPublicacaoResponse.invalido(e.getMessage());
        }

        ReferenciaTarefa ref = referencia.orElse(null);

        return new DestinoPublicacaoResponse(
                ref == null ? null : ref.provedor().name(),
                ref == null ? null : ref.identificador(),
                resolverProjectKey(ref, projectKey),
                resolverPastaRaiz(ref, pastaDestino),
                true,
                null
        );
    }

    private String resolverProjectKey(ReferenciaTarefa ref, String doPedido) {
        if (temTexto(doPedido)) {
            return doPedido.trim();
        }
        if (ref != null && ref.provedor() == ProvedorTarefa.JIRA) {
            return ref.projeto();
        }
        return zephyrProperties.getProjectKey();
    }

    private String resolverPastaRaiz(ReferenciaTarefa ref, String doPedido) {
        if (temTexto(doPedido)) {
            return doPedido.trim();
        }

        String derivada = folderStrategyResolver.resolverPastaRaiz(ref);
        return temTexto(derivada) ? derivada : zephyrProperties.getRootFolder();
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
