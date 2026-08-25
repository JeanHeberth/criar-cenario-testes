package com.br.criarcenariotestes.business.autoqa.review;


import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nomes de campo que o CENÁRIO menciona — o vocabulário legítimo do contrato.
 *
 * <p>Existe por um defeito observado em produção: o código gerado passou em
 * todas as regras estáticas e mesmo assim asseria um contrato inventado
 * (esperava "message" onde o contrato dizia "mensagem"). As regras verificavam
 * a FORMA do teste — separação de camadas, mensagem em assertion, ausência de
 * segredo — e nenhuma verificava se o teste falava da API certa. Teste que
 * assere campo inexistente é plausível, passa na revisão e falha na execução,
 * apontando para o lugar errado.
 *
 * <p>A fonte é o TEXTO ORIGINAL do cenário — o que a pessoa escreveu —, nunca a
 * ScenarioAnalysisResult. A primeira versão desta classe lia da análise e não
 * acusou nada: o modelo já havia trocado "erro"/"mensagem" por "error"/"message"
 * na própria análise, a geração seguiu fielmente, e o check comparou uma cópia
 * com outra cópia. Ambas concordavam e ambas estavam erradas. Validar contra a
 * releitura da IA não valida coisa alguma.
 *
 * <p>Deliberadamente conservador: só reconhece nome, nunca posição. Saber que
 * "path" existe no contrato não é saber que ele pertence à resposta 200 — isso
 * exigiria um contrato estruturado por status. Esta classe pega o campo
 * INVENTADO; o campo certo no lugar errado continua passando.
 */
public final class VocabularioDoContrato {

    /**
     * Abaixo disso a análise foi pobre demais para servir de referência, e
     * bloquear com base nela reprovaria código correto. Sem vocabulário
     * confiável a regra se cala — não é papel dela adivinhar.
     */
    private static final int MINIMO_PARA_CONFIAR = 3;

    private static final Pattern IDENTIFICADOR_CITADO =
            Pattern.compile("[\"']([A-Za-z_][A-Za-z0-9_]{1,40})[\"']");

    private final Set<String> campos;

    private VocabularioDoContrato(Set<String> campos) {
        this.campos = campos;
    }

    /**
     * @param textoDoCenario texto original do cenário, como enviado à execução.
     */
    public static VocabularioDoContrato doTexto(String textoDoCenario) {
        Set<String> encontrados = new LinkedHashSet<>();
        extrairDeTexto(encontrados, textoDoCenario);
        return new VocabularioDoContrato(encontrados);
    }

    /**
     * Só há vocabulário utilizável quando a análise citou campos suficientes.
     */
    public boolean utilizavel() {
        return campos.size() >= MINIMO_PARA_CONFIAR;
    }

    public boolean conhece(String campo) {
        return campo != null && campos.contains(normalizar(campo));
    }

    public Set<String> campos() {
        return Set.copyOf(campos);
    }

    /**
     * Só entram identificadores CITADOS entre aspas — é assim que um contrato
     * nomeia campo ("email", "senha"). Varrer toda palavra da prosa encheria o
     * vocabulário de ruído e a regra nunca acusaria nada: no contrato de login
     * real, esta regra extrai 15 nomes, todos corretos, e nenhum lixo.
     */
    private static void extrairDeTexto(Set<String> destino, String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        Matcher matcher = IDENTIFICADOR_CITADO.matcher(texto);
        while (matcher.find()) {
            adicionar(destino, matcher.group(1));
        }
    }

    private static void adicionar(Set<String> destino, String campo) {
        if (campo == null || campo.isBlank()) {
            return;
        }
        destino.add(normalizar(campo));
    }

    private static String normalizar(String campo) {
        return campo.trim().toLowerCase(Locale.ROOT);
    }

    public static List<String> nomesDesconhecidos(VocabularioDoContrato contrato, Collection<String> candidatos) {
        return candidatos.stream().filter(nome -> !contrato.conhece(nome)).distinct().toList();
    }

    /**
     * Status HTTP que o cenário realmente define, extraídos do texto original.
     *
     * <p>O cenário marca alguns comportamentos como exploratórios — "registrar
     * o comportamento observado" — e o modelo os converte em asserção com um
     * palpite. Aconteceu com o método não permitido: o teste gerado afirmou
     * 405 ou 404, e a API responde 500. O teste falharia na primeira execução
     * por causa de uma suposição, não de um defeito do sistema testado.
     */
    public static java.util.Set<String> statusDefinidos(String textoDoCenario) {
        if (textoDoCenario == null || textoDoCenario.isBlank()) {
            return java.util.Set.of();
        }
        java.util.Set<String> encontrados = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("\\b([1-5][0-9]{2})\\b").matcher(textoDoCenario);
        while (matcher.find()) {
            encontrados.add(matcher.group(1));
        }
        return java.util.Set.copyOf(encontrados);
    }
}
