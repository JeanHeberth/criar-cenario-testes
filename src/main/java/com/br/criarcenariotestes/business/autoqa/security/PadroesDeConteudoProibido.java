package com.br.criarcenariotestes.business.autoqa.security;

import java.util.regex.Pattern;

/**
 * Padrões que identificam conteúdo que a IA não deveria devolver em campos de
 * texto: código, comandos de shell e caminhos do filesystem.
 *
 * Ficam num lugar só porque estavam duplicados em ScenarioAnalysisValidator e
 * PlanningValidator, e a duplicata cobrou caro: o mesmo falso positivo teve de
 * ser caçado duas vezes, em etapas diferentes do workflow, gastando uma rodada
 * de chamadas de IA a cada diagnóstico. Uma terceira cópia divergindo é
 * questão de tempo.
 *
 * O princípio nos três: reconhecer a FORMA do que se quer barrar, não a mera
 * menção. Texto de análise fala de classes, cita ferramentas e descreve rotas
 * — nada disso é código, comando ou caminho de disco.
 */
public final class PadroesDeConteudoProibido {

    private PadroesDeConteudoProibido() {
    }

    /**
     * Código-fonte. ";" só conta fechando linha — em prosa é pontuação comum
     * ("o usuário adiciona; o sistema valida"), e barrá-lo reprovava análises
     * corretas.
     *
     * <p>Chaves ({@code {} }) saíram pelo mesmo motivo, um nível acima: não são
     * sinal de código, são sinal de JSON. Um cenário de teste de API descreve o
     * corpo da requisição e da resposta — {@code testData[].example} de um POST
     * é literalmente <code>{"email": "...", "senha": "..."}</code>. Com a chave
     * na regra, era impossível analisar qualquer contrato REST: a análise
     * inteira era descartada e o workflow travava no primeiro estágio.
     *
     * <p>Nada de real se perde. Código de verdade traz keyword, "=>" ou ";"
     * fechando linha; o que sobrava só com chave era justamente o caso ambíguo
     * — literal de objeto e payload JSON são indistinguíveis. E estes padrões
     * guardam CAMPOS DE DESCRIÇÃO (ScenarioAnalysisValidator, PlanningValidator),
     * não os arquivos gerados, que têm validação própria na geração e no review.
     */
    public static final Pattern CODIGO = Pattern.compile(
            "(?i)\\b(class|import|public|private|protected|return|function|def|let|const|var|new)\\b"
                    + "|=>|;\\s*$",
            Pattern.MULTILINE);

    /**
     * Comando de shell, reconhecido pelo que SEGUE a ferramenta (flag, URL ou
     * subcomando). "o projeto usa gradle" é descrição; "gradle test --tests X"
     * é comando.
     */
    public static final Pattern COMANDO = Pattern.compile(
            "(?i)\\b(curl|npm|yarn|pnpm|mvn|gradle|git|docker|kubectl|chmod|ssh)\\s+"
                    + "(-{1,2}\\w|https?://|\\d{3,4}\\s|(?:test|run|install|build|clone|push|pull|exec|start|"
                    + "compose|apply|add|commit|init|clean|deploy)\\b)");

    /**
     * Caminho de filesystem Unix, reconhecido pela RAIZ do sistema. Casar
     * qualquer "/token" reprovava rota HTTP ("/cenario/workflows") e
     * inviabilizava cenários de teste de API.
     */
    public static final Pattern CAMINHO_UNIX = Pattern.compile(
            "(?<!\\S)/(?:Users|home|root|var|etc|tmp|opt|usr|bin|sbin|private|mnt|media|srv|proc|dev"
                    + "|Applications|Library|System|Volumes)(?:/[^\\s]*)?");

    /**
     * Segredo hardcoded: chave sensível seguida de VALOR LITERAL entre aspas.
     *
     * As aspas são a regra, não detalhe: valor sem aspas é referência
     * (process.env.SENHA, VALID_PASSWORD, System.getenv("X")) — justamente o
     * que queremos que o código gerado use. A versão que aceitava "\\S+"
     * qualquer reprovava a revisão inteira quando a IA citava
     * "password: VALID_PASSWORD" como evidência de um achado.
     */
    /**
     * O "\\w*" antes da chave cobre camelCase e prefixos: invalidPassword,
     * userSenha, adminToken. Sem ele, "\\b(password)\\b" não casa dentro de
     * "invalidPassword" — não há fronteira de palavra entre "d" e "P" —, e era
     * exatamente essa a forma que o gerador produzia para o caso negativo.
     */
    private static final Pattern SEGREDO_CHAVE_VALOR = Pattern.compile(
            "(?i)\\b\\w*(password|senha|secret|token|apikey|api[_-]?key|private_key)\\b\\s*[:=]\\s*"
                    + "[\"']([^\"'`\\r\\n]{3,})[\"']");

    /** Marcas de seletor CSS/XPath — não aparecem num segredo de verdade. */
    private static final Pattern SELETOR = Pattern.compile(
            "^[.#/\\[]|\\[|=|\\binput\\b|\\bbutton\\b|\\bdata-|::");

    /**
     * Palavra minúscula simples, sem dígito nem maiúscula: "senha_errada",
     * "senha_incorreta", "wrong_password". É a credencial propositalmente
     * inválida que todo caso negativo precisa ter — não é segredo a proteger.
     *
     * O critério é estrutural em vez de lista de palavras porque a lista falha:
     * enumerei "errada/invalid/wrong" e na execução seguinte a IA escreveu
     * "senha_incorreta". Segredo real carrega entropia — maiúscula, dígito ou
     * prefixo conhecido — e continua barrado.
     */
    private static final Pattern PALAVRA_SIMPLES = Pattern.compile("^[a-zà-ÿ][a-zà-ÿ_\\-. ]*$");

    private static final Pattern PLACEHOLDER = Pattern.compile(
            "(?i)^x+$|^\\*+$|^<.*>$|^\\{\\{.*\\}\\}$|^changeme$|^todo$");

    /**
     * Substitui por {@code [REDACTED]} apenas os segredos LITERAIS, preservando
     * referências.
     *
     * Redigir indiscriminadamente tudo depois de "password:" transformava
     * {@code const password = process.env.AUTH_PASSWORD;} em
     * {@code const password = [REDACTED];} antes de o código chegar à IA
     * revisora — que então acusava HARDCODED_SECRET (CRÍTICO) e bloqueava a
     * aplicação. Ou seja: a redação fabricava a violação que existe para
     * prevenir, num código que estava correto.
     */
    public static String redigirSegredosLiterais(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        var matcher = SEGREDO_CHAVE_VALOR.matcher(texto);
        StringBuilder saida = new StringBuilder();
        while (matcher.find()) {
            String valor = matcher.group(2).trim();
            boolean redigir = !(valor.isEmpty()
                    || SELETOR.matcher(valor).find()
                    || PLACEHOLDER.matcher(valor).find()
                    || PALAVRA_SIMPLES.matcher(valor).matches());
            String substituto = redigir
                    ? matcher.group(0).replace(matcher.group(2), "[REDACTED]")
                    : matcher.group(0);
            matcher.appendReplacement(saida, java.util.regex.Matcher.quoteReplacement(substituto));
        }
        matcher.appendTail(saida);
        return saida.toString();
    }

    /** Único ponto de decisão sobre "isto é segredo hardcoded?". */
    public static boolean contemSegredoLiteral(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        var matcher = SEGREDO_CHAVE_VALOR.matcher(texto);
        while (matcher.find()) {
            String valor = matcher.group(2).trim();
            if (valor.isEmpty()
                    || SELETOR.matcher(valor).find()
                    || PLACEHOLDER.matcher(valor).find()
                    || PALAVRA_SIMPLES.matcher(valor).matches()) {
                continue;
            }
            return true;
        }
        return false;
    }
}
