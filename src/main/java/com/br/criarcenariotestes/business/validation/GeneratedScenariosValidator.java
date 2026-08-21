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
    /**
     * FASE15-BUG-003C: o sufixo "[ \t]*" tolera INDENTAÇÃO antes da keyword.
     * Sem ele, um cenário BDD íntegro era reprovado só porque o modelo
     * indentou as linhas seguintes à primeira (observado com gemini-2.5-flash:
     * "Dado ...\n  Quando ...\n  Então ..." dava Dado=true, Quando=false,
     * Então=false). Indentação é whitespace insignificante e não muda a
     * semântica do passo. Bullets ("- ", "* ") e passos numerados continuam
     * NÃO aceitos de propósito - essa restrição é deliberada (FASE15-BUG-003)
     * e segue valendo, porque aí muda a estrutura, não só o recuo.
     */
    private static final String INICIO_DE_PASSO = "(?:^|\\n|(?<=[.;:)])\\s+)[ \\t]*";
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

            ValidationResult evidenciaResult = validarEstruturaEvidencia(item);
            if (!evidenciaResult.valido()) {
                return ValidationResult.invalido(
                        "Cenário '" + item.getNome() + "' com evidência estruturalmente inválida: " + evidenciaResult.motivo());
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

    /**
     * Um cenário é APROVEITÁVEL quando tem nome, Passos e Resultado Esperado —
     * ou seja, quando um humano consegue ler e executar o teste. É o critério
     * que separa "irrecuperável" (descartar) de "apenas malformado" (manter e
     * marcar para revisão) na segregação pré-persistência.
     *
     * <p>Complementa exatamente {@link #validarRepresentacaoFinal}: a única
     * reprovação de lá que sobra aqui como aproveitável é a falta das keywords
     * Dado/Quando, que é defeito de FORMATAÇÃO, não de conteúdo. Esta classe
     * declara no próprio Javadoc que sua verificação é estrutural e não
     * semântica — não cabe a ela descartar conteúdo por causa de uma quebra de
     * linha ausente.
     */
    public boolean temConteudoAproveitavel(CenarioItem item) {
        return item != null
                && temTexto(item.getNome())
                && temTexto(item.getScriptTeste())
                && temTexto(item.getResultadoEsperado());
    }

    /**
     * FASE15-BUG-005B: valida estruturalmente a rastreabilidade de evidência
     * declarada pelo Generator. Cenários legados (evidenceType nulo/vazio)
     * não possuem essa informação e não são invalidados por isso —
     * retrocompatibilidade. Quando presente, evidenceType precisa ser um dos
     * três valores conhecidos; EXPLORATORY exige Status=REVIEW_REQUIRED;
     * DOCUMENTED/DIRECT_INFERENCE exigem evidenceSources preenchido.
     */
    public ValidationResult validarEstruturaEvidencia(CenarioItem item) {
        String tipo = item.getEvidenceType();
        if (!temTexto(tipo)) {
            return ValidationResult.ok();
        }

        if (!tipo.equals("DOCUMENTED") && !tipo.equals("DIRECT_INFERENCE") && !tipo.equals("EXPLORATORY")) {
            return ValidationResult.invalido("evidenceType desconhecido: '" + tipo + "'.");
        }

        if (tipo.equals("EXPLORATORY") && !"REVIEW_REQUIRED".equals(item.getStatus())) {
            return ValidationResult.invalido("Cenário EXPLORATORY deve ter Status REVIEW_REQUIRED.");
        }

        if ((tipo.equals("DOCUMENTED") || tipo.equals("DIRECT_INFERENCE")) && !temTexto(item.getEvidenceSources())) {
            return ValidationResult.invalido(tipo + " requer evidenceSources preenchido com uma fonte real.");
        }

        return ValidationResult.ok();
    }

    /**
     * FASE15-BUG-005B: checagem determinística de existência literal de uma
     * fonte citada no texto bruto de regraDeNegocio. "USER" é uma referência
     * especial à regra digitada pelo usuário — validada pela existência de
     * conteúdo de entrada, não por busca de string literal "USER".
     */
    public boolean fonteExisteNoTextoBruto(String fonte, String regraDeNegocioBruta) {
        return fonteExisteNoTextoBruto(fonte, regraDeNegocioBruta, null);
    }

    /**
     * FASE15-BUG-005B (continuação — correção do rebaixamento excessivo de
     * fontes RF/RNF): além do texto documental bruto, reconhece como
     * existente uma fonte citada literalmente nos requisitos derivados
     * (RF/RNF) produzidos pelo RequirementAnalysisAgent PARA ESTA MESMA
     * EXECUÇÃO — nunca de um catálogo global ou de outra execução. Não
     * aceita nada apenas pelo formato (ex.: "RF\d+"); exige presença
     * literal no texto de requisitos realmente gerado.
     */
    public boolean fonteExisteNoTextoBruto(String fonte, String regraDeNegocioBruta, String requisitosDerivados) {
        if (!temTexto(fonte)) {
            return false;
        }
        String fonteTrim = fonte.trim();
        if ("USER".equalsIgnoreCase(fonteTrim)) {
            return temTexto(regraDeNegocioBruta);
        }
        if (regraDeNegocioBruta != null && regraDeNegocioBruta.contains(fonteTrim)) {
            return true;
        }
        return requisitosDerivados != null && requisitosDerivados.contains(fonteTrim);
    }

    /**
     * FASE15-BUG-005B: correção determinística fail-closed — nunca deixar
     * APPROVED + fonte inexistente. Se qualquer fonte citada não existir no
     * texto bruto, rebaixa o item inteiro para EXPLORATORY/REVIEW_REQUIRED,
     * sem disparar novo retry (correção pontual do item, não da geração).
     */
    public void corrigirSourceInexistente(CenarioItem item, String regraDeNegocioBruta) {
        corrigirSourceInexistente(item, regraDeNegocioBruta, null);
    }

    /**
     * FASE15-BUG-005B (continuação): mesma correção fail-closed, agora
     * também aceitando fontes RF/RNF presentes nos requisitos derivados
     * desta execução (ver {@link #fonteExisteNoTextoBruto(String, String, String)}).
     */
    public void corrigirSourceInexistente(CenarioItem item, String regraDeNegocioBruta, String requisitosDerivados) {
        String tipo = item.getEvidenceType();
        if (!"DOCUMENTED".equals(tipo) && !"DIRECT_INFERENCE".equals(tipo)) {
            return;
        }

        String fontes = item.getEvidenceSources();
        if (!temTexto(fontes)) {
            return;
        }

        for (String parte : fontes.split(",")) {
            if (!fonteExisteNoTextoBruto(parte.trim(), regraDeNegocioBruta, requisitosDerivados)) {
                item.setEvidenceType("EXPLORATORY");
                item.setStatus("REVIEW_REQUIRED");
                return;
            }
        }
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
