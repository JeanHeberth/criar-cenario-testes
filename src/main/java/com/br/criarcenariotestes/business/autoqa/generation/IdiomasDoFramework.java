package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Corrige construções que NÃO EXISTEM no framework, no código já gerado.
 *
 * <p>Cinco regerações seguidas mostraram que instrução de prompt não elimina
 * esta classe de erro: o modelo trocou {@code data:} por {@code body:} nas
 * cinco, mesmo recebendo a mensagem do compilador com arquivo, linha e coluna.
 * Realimentar o erro diz ONDE está errado, não QUAL é a forma certa — e o
 * modelo tentava outra coisa a cada volta, oscilando entre 1 e 9 erros.
 *
 * <p>Cada regra aqui obedece a um critério rígido: só entra o que tem UMA
 * forma correta e nenhuma ambiguidade. {@code body} não é opção válida do
 * APIRequestContext e {@code playwright} não é export de @playwright/test —
 * nos dois casos não existe leitura alternativa em que o código original
 * estivesse certo. Preferência de estilo não entra: isso é do autor do teste.
 *
 * <p>Toda correção é registrada em log. O sistema reescrever código gerado é
 * decisão consciente, não pode acontecer em silêncio.
 */
@Component
public class IdiomasDoFramework {

    private static final Logger log = LoggerFactory.getLogger(IdiomasDoFramework.class);

    /**
     * {@code body:} dentro das opções de uma chamada de requisição.
     * O APIRequestContext aceita {@code data}, {@code form}, {@code multipart},
     * {@code headers}, {@code params}, {@code timeout} — nunca {@code body}.
     * O limite de 400 caracteres e a parada no ";" mantêm a troca dentro da
     * mesma chamada, sem alcançar um {@code body:} de outro contexto.
     */
    private static final Pattern BODY_EM_REQUISICAO = Pattern.compile(
            "(\\.(?:post|put|patch|delete|fetch|head|get)\\s*\\([^;]{0,400}?)\\bbody(\\s*:)",
            Pattern.DOTALL);

    /** "playwright" na lista de imports nomeados — não é export do pacote. */
    private static final Pattern IMPORT_PLAYWRIGHT = Pattern.compile(
            "(import\\s*\\{)([^}]*)(\\}\\s*from\\s*['\"]@playwright/test['\"])");

    public record Correcao(String conteudo, List<String> aplicadas) {}

    public Correcao aplicar(AutomationFramework framework, String relativePath, String conteudo) {
        if (framework != AutomationFramework.PLAYWRIGHT || conteudo == null || conteudo.isBlank()) {
            return new Correcao(conteudo, List.of());
        }

        List<String> aplicadas = new ArrayList<>();
        String resultado = trocarBodyPorData(conteudo, aplicadas);
        resultado = removerImportDePlaywright(resultado, aplicadas);

        if (!aplicadas.isEmpty()) {
            log.warn("Idiomas corrigidos no código gerado. arquivo='{}', correções={}", relativePath, aplicadas);
        }
        return new Correcao(resultado, List.copyOf(aplicadas));
    }

    private String trocarBodyPorData(String conteudo, List<String> aplicadas) {
        Matcher matcher = BODY_EM_REQUISICAO.matcher(conteudo);
        StringBuilder saida = new StringBuilder();
        int trocas = 0;
        while (matcher.find()) {
            matcher.appendReplacement(saida, Matcher.quoteReplacement(matcher.group(1) + "data" + matcher.group(2)));
            trocas++;
        }
        matcher.appendTail(saida);
        if (trocas > 0) {
            aplicadas.add("body: → data: (" + trocas + "x)");
        }
        return saida.toString();
    }

    private String removerImportDePlaywright(String conteudo, List<String> aplicadas) {
        Matcher matcher = IMPORT_PLAYWRIGHT.matcher(conteudo);
        if (!matcher.find()) {
            return conteudo;
        }
        String nomes = matcher.group(2);
        List<String> mantidos = new ArrayList<>();
        boolean removeu = false;
        for (String nome : nomes.split(",")) {
            String limpo = nome.trim();
            if (limpo.isEmpty()) {
                continue;
            }
            if (limpo.equals("playwright")) {
                removeu = true;
                continue;
            }
            mantidos.add(limpo);
        }
        if (!removeu) {
            return conteudo;
        }
        aplicadas.add("removido 'playwright' dos imports nomeados");
        String novo = matcher.group(1) + " " + String.join(", ", mantidos) + " " + matcher.group(3);
        return conteudo.substring(0, matcher.start()) + novo + conteudo.substring(matcher.end());
    }
}
