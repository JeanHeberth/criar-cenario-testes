package com.br.criarcenariotestes.business.tracker;

import com.br.criarcenariotestes.business.properties.FolderStrategyProperties;
import com.br.criarcenariotestes.business.properties.ZephyrProperties;
import com.br.criarcenariotestes.infrastructure.jira.DadosDaIssue;
import com.br.criarcenariotestes.infrastructure.jira.JiraClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;

/**
 * Deriva a pasta raiz de destino a partir da tarefa informada no pedido,
 * lendo campos que o time já mantém no rastreador.
 *
 * Isto substitui, de propósito, a alternativa de pedir para a IA inferir a
 * stack a partir do título e da regra de negócio: essa informação não está no
 * requisito (é decisão do time sobre a automação), e um chute errado cria
 * pasta permanente — a API do Zephyr não expõe remoção (405). Aqui a regra é
 * declarada, o mapa de destinos é fechado, e o que não casa cai no fallback
 * em vez de virar pasta nova.
 */
@Component
@RequiredArgsConstructor
public class FolderStrategyResolver {

    private static final Logger log = LoggerFactory.getLogger(FolderStrategyResolver.class);

    private final ZephyrProperties zephyrProperties;
    private final JiraClient jiraClient;

    /**
     * @return o nome da pasta derivada, ou null quando a estratégia está
     *         desligada, a referência não serve, ou nada casou com o mapa —
     *         cabe ao chamador aplicar sua própria precedência/fallback.
     */
    public String resolverPastaRaiz(ReferenciaTarefa referencia) {
        FolderStrategyProperties estrategia = zephyrProperties.getFolderStrategy();

        if (!estrategia.isEnabled() || referencia == null) {
            return null;
        }

        // Só Jira por enquanto: ler campos do Azure DevOps exigiria o
        // adaptador de work items, ainda não implementado.
        if (referencia.provedor() != ProvedorTarefa.JIRA) {
            log.debug("Estratégia de pasta ignorada: referência é do {}.", referencia.provedor());
            return null;
        }

        DadosDaIssue issue;
        try {
            issue = jiraClient.buscarDadosDaIssue(referencia.identificador());
        } catch (Exception e) {
            // A geração já terminou; perder a pasta derivada é aceitável,
            // perder a publicação por indisponibilidade do Jira não é.
            log.warn("Falha ao ler a issue '{}' para derivar a pasta - seguindo com o destino padrão. erro={}",
                    referencia.identificador(), e.getMessage());
            return null;
        }

        for (String fonte : estrategia.getSources()) {
            String pasta = resolverPorFonte(fonte, issue, estrategia.getMapping());
            if (pasta != null) {
                log.info("Pasta '{}' derivada da issue {} pela fonte '{}'.", pasta, issue.key(), fonte);
                return pasta;
            }
        }

        log.info("Nenhuma fonte da issue {} casou com o mapa de pastas configurado - usando o destino padrão.",
                issue.key());
        return null;
    }

    private String resolverPorFonte(String fonte, DadosDaIssue issue, Map<String, String> mapa) {
        if (fonte == null) {
            return null;
        }

        return switch (fonte.trim().toLowerCase()) {
            case "components" -> primeiroMapeado(issue.componentes(), mapa);
            case "labels" -> primeiroMapeado(issue.labels(), mapa);
            // O summary é texto livre: procuramos os termos do mapa DENTRO
            // dele, em vez de exigir igualdade. Fonte de último recurso, para
            // projetos que ainda escrevem a stack no título.
            case "summary" -> termoContidoNoTexto(issue.summary(), mapa);
            default -> {
                log.warn("Fonte de pasta desconhecida na configuração: '{}'. Ignorando.", fonte);
                yield null;
            }
        };
    }

    private String primeiroMapeado(List<String> valores, Map<String, String> mapa) {
        if (valores == null) {
            return null;
        }

        for (String valor : valores) {
            String destino = buscarNoMapa(valor, mapa);
            if (destino != null) {
                return destino;
            }
        }
        return null;
    }

    private String termoContidoNoTexto(String texto, Map<String, String> mapa) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        String normalizado = normalizar(texto);
        for (Map.Entry<String, String> entrada : mapa.entrySet()) {
            String termo = normalizar(entrada.getKey());
            if (!termo.isBlank() && contemPalavra(normalizado, termo)) {
                return entrada.getValue();
            }
        }
        return null;
    }

    /**
     * Casa o termo como palavra inteira. Sem isso, "java" acharia "javascript"
     * e mandaria o caso para a pasta errada — permanente, no Zephyr.
     */
    private boolean contemPalavra(String textoNormalizado, String termoNormalizado) {
        return textoNormalizado.matches(".*\\b" + java.util.regex.Pattern.quote(termoNormalizado) + "\\b.*");
    }

    private String buscarNoMapa(String valor, Map<String, String> mapa) {
        if (valor == null) {
            return null;
        }

        String alvo = normalizar(valor);
        for (Map.Entry<String, String> entrada : mapa.entrySet()) {
            if (normalizar(entrada.getKey()).equals(alvo)) {
                return entrada.getValue();
            }
        }
        return null;
    }

    /** Minúsculas e sem acento: "Automação" e "automacao" são o mesmo termo. */
    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return semAcento.trim().toLowerCase();
    }
}
