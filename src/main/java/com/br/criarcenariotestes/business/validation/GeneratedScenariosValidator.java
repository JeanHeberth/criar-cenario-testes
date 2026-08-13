package com.br.criarcenariotestes.business.validation;

import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * FASE15-BUG-002: valida estruturalmente a saída da IA (resposta bruta e
 * cenários já extraídos) antes que ela siga para o Redundancy Reviewer ou
 * seja persistida. Verificação estrutural, não semântica — não avalia se o
 * conteúdo do cenário está correto, apenas se "parece" um cenário de teste.
 */
@Component
public class GeneratedScenariosValidator {

    private static final Pattern PADRAO_PLANO_DE_ARQUIVOS = Pattern.compile(
            "(?i)plano de gera[çc][ãa]o|arquivos?\\s+a\\s+criar|pasta\\s+base"
    );

    /**
     * FASE15-BUG-003/FASE15-BUG-003A: keyword BDD só conta quando inicia um
     * passo — no começo do texto, logo após uma quebra de linha, ou logo após
     * pontuação de fim de frase/fechamento de parêntese (". "/"; "/": "/") ").
     * Isso evita falso positivo com a palavra no meio de uma frase (ex.: "o
     * sistema bloqueia quando ocorrer..."). A proteção real contra falso
     * positivo é a keyword exigir inicial maiúscula com `\b` — "quando(" em
     * minúsculo colado a um parêntese (ex.: chamada de função) nunca casa.
     */
    private static final String INICIO_DE_PASSO = "(?:^|\\n|(?<=[.;:)])\\s+)";
    private static final Pattern KEYWORD_DADO =
            Pattern.compile(INICIO_DE_PASSO + "(?:Dado que|Dado)\\b");
    private static final Pattern KEYWORD_QUANDO =
            Pattern.compile(INICIO_DE_PASSO + "Quando\\b");
    private static final Pattern KEYWORD_ENTAO =
            Pattern.compile(INICIO_DE_PASSO + "Então\\b");

    public ValidationResult validarRespostaBruta(String respostaBruta) {
        if (respostaBruta == null || respostaBruta.isBlank()) {
            return ValidationResult.invalido("Resposta da IA vazia.");
        }
        if (PADRAO_PLANO_DE_ARQUIVOS.matcher(respostaBruta).find()) {
            return ValidationResult.invalido(
                    "Resposta contém um plano de arquivos/geração em vez de cenários de teste.");
        }
        return ValidationResult.ok();
    }

    public ValidationResult validarGeracao(String respostaBruta, List<CenarioItem> cenarios) {
        ValidationResult respostaResult = validarRespostaBruta(respostaBruta);
        if (!respostaResult.valido()) {
            return respostaResult;
        }

        if (cenarios == null || cenarios.isEmpty()) {
            return ValidationResult.invalido("Nenhum cenário de teste foi extraído da resposta.");
        }

        for (CenarioItem item : cenarios) {
            boolean temConteudoMinimo = temTexto(item.getObjetivo())
                    || temTexto(item.getScriptTeste())
                    || temTexto(item.getResultadoEsperado());

            if (!temTexto(item.getNome()) || !temConteudoMinimo) {
                return ValidationResult.invalido(
                        "Cenário sem conteúdo estrutural mínimo (nome e ao menos objetivo/passos/resultado esperado).");
            }

            ValidationResult bddResult = validarEstruturaBdd(item.getScriptTeste());
            if (!bddResult.valido()) {
                return ValidationResult.invalido(
                        "Cenário '" + item.getNome() + "' não está em formato BDD: " + bddResult.motivo());
            }
        }

        return ValidationResult.ok();
    }

    /**
     * FASE15-BUG-003: um cenário BDD válido precisa ter, no mínimo, um
     * "Dado"/"Dado que", um "Quando" e um "Então" iniciando um passo — não
     * basta a palavra aparecer em qualquer lugar do texto.
     */
    public ValidationResult validarEstruturaBdd(String passos) {
        if (passos == null || passos.isBlank()) {
            return ValidationResult.invalido("Passos vazios — não é possível validar estrutura BDD.");
        }

        boolean temDado = KEYWORD_DADO.matcher(passos).find();
        boolean temQuando = KEYWORD_QUANDO.matcher(passos).find();
        boolean temEntao = KEYWORD_ENTAO.matcher(passos).find();

        if (temDado && temQuando && temEntao) {
            return ValidationResult.ok();
        }

        return ValidationResult.invalido(String.format(
                "estrutura BDD ausente ou incompleta (Dado=%s, Quando=%s, Então=%s).",
                temDado, temQuando, temEntao));
    }

    /**
     * FASE15-BUG-003A: validação determinística executada imediatamente antes
     * da persistência, sobre a representação FINAL do cenário (já processada
     * pelo BddFormatterAgent). Diferente de {@link #validarEstruturaBdd}, não
     * exige "Então" literalmente em Passos — nessa etapa o Formatter já deve
     * ter movido esse conteúdo para Resultado Esperado; exige apenas que o
     * campo não esteja vazio.
     */
    public ValidationResult validarRepresentacaoFinal(CenarioItem item) {
        if (item == null || !temTexto(item.getNome())) {
            return ValidationResult.invalido("Cenário sem nome/título válido.");
        }
        if (!temTexto(item.getScriptTeste())) {
            return ValidationResult.invalido("Cenário '" + item.getNome() + "' com Passos vazio.");
        }
        if (!temTexto(item.getResultadoEsperado())) {
            return ValidationResult.invalido("Cenário '" + item.getNome() + "' com Resultado Esperado vazio.");
        }

        boolean temDado = KEYWORD_DADO.matcher(item.getScriptTeste()).find();
        boolean temQuando = KEYWORD_QUANDO.matcher(item.getScriptTeste()).find();

        if (!temDado || !temQuando) {
            return ValidationResult.invalido(String.format(
                    "Cenário '%s' sem estrutura BDD mínima nos Passos (Dado=%s, Quando=%s).",
                    item.getNome(), temDado, temQuando));
        }

        return ValidationResult.ok();
    }

    /**
     * FASE15-BUG-003A: identifica, de forma puramente estrutural (sem
     * blacklist de nomes/palavras), um item pós-Reviewer que não é um
     * cenário real — apenas conteúdo editorial/comentário (ex.: "Observações
     * de otimização:") que não deveria ter sido tratado como CenarioItem.
     * Critério: nenhum conteúdo real em Passos NEM em Resultado Esperado,
     * independentemente do que está no nome.
     */
    public boolean pareceConteudoNaoCenario(CenarioItem item) {
        return item != null
                && !temTexto(item.getScriptTeste())
                && !temTexto(item.getResultadoEsperado());
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    public record ValidationResult(boolean valido, String motivo) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalido(String motivo) {
            return new ValidationResult(false, motivo);
        }
    }
}
