package com.br.criarcenariotestes.business.properties;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Como derivar a pasta de destino a partir da tarefa informada no pedido.
 *
 * O objetivo é reaproveitar a taxonomia que o time já mantém no rastreador,
 * em vez de inventar uma organização paralela — e fazer isso por regra
 * declarada, não por inferência: a mesma tarefa sempre resolve para a mesma
 * pasta, e quando alguém questionar por que um caso foi parar ali, a resposta
 * é esta configuração.
 */
@Getter
@Setter
public class FolderStrategyProperties {

    /** Desligado por padrão: sem configurar, nada muda no comportamento atual. */
    private boolean enabled = false;

    /**
     * Campos da issue consultados, em ordem de precedência. O primeiro que
     * produzir uma pasta conhecida vence.
     *
     * Num Jira corporativo bem mantido, "components" é a fonte natural — é o
     * campo que carrega a stack/área. "summary" existe como último recurso
     * para projetos que ainda não preenchem campo estruturado e escrevem a
     * stack no título ("Automacao POSTMAN do POST Usuario"); é mais frágil,
     * porque depende de convenção de escrita, e deve ser tratado como degrau
     * de migração, não como destino.
     */
    private List<String> sources = List.of("components", "labels");

    /**
     * Mapa fechado de termo reconhecido -> nome da pasta. Comparação sem
     * diferenciar maiúsculas/acentos.
     *
     * Ser FECHADO é a salvaguarda que separa isto de deixar a IA adivinhar:
     * um valor que não está no mapa não vira pasta nova, cai no fallback. Sem
     * isso, cada variação escrita por um time ("Auth", "Autenticação") criaria
     * uma pasta irmã — e pasta no Zephyr não tem DELETE via API.
     */
    private Map<String, String> mapping = new LinkedHashMap<>();
}
