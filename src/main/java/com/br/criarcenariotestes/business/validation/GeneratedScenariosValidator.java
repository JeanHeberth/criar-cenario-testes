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
     * FASE15-BUG-003: keyword BDD só conta quando inicia um passo — no começo
     * do texto, logo após uma quebra de linha, ou logo após pontuação de fim
     * de frase (". "/"; "/": "). Isso evita falso positivo com a palavra no
     * meio de uma frase (ex.: "o sistema bloqueia quando ocorrer...").
     */
    private static final String INICIO_DE_PASSO = "(?:^|\\n|(?<=[.;:])\\s+)";
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
