package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class BddFormatterAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(BddFormatterAgent.class);
    
    private static final Pattern BDD_PATTERN = Pattern.compile(
        "(Dado que|Dado|E|Quando|Então)",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public void executar(WorkflowContext context) {
        log.info("Iniciando formatação BDD. titulo='{}'", context.getRequest().titulo());
        
        List<CenarioItem> cenarios = context.getCenariosRevisados();
        if (cenarios == null || cenarios.isEmpty()) {
            cenarios = context.getCenarios();
        }
        
        if (cenarios == null || cenarios.isEmpty()) {
            log.warn("Nenhum cenário para formatar em BDD.");
            return;
        }
        
        try {
            for (CenarioItem item : cenarios) {
                formatarCenarioBdd(item);
            }

            log.info("Formatação BDD concluída. cenarios={}", cenarios.size());
            
        } catch (Exception e) {
            log.error("Erro ao formatar BDD: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getNome() {
        return "BDD Formatter";
    }
    
    private void formatarCenarioBdd(CenarioItem item) {
        String scriptOriginal = item.getScriptTeste();
        String resultadoOriginal = item.getResultadoEsperado();
        
        // Combinar script + resultado para processar junto
        String textoCompleto = combinarTextos(scriptOriginal, resultadoOriginal);
        
        if (textoCompleto == null || textoCompleto.isBlank()) {
            return;
        }
        
        // Separar em partes BDD
        SeparacaoBdd separacao = separarTextoBdd(textoCompleto);
        
        // Atualizar campos do item
        if (separacao.temPassos()) {
            item.setScriptTeste(separacao.getPassosFormatados());
        }

        if (separacao.temResultados()) {
            item.setResultadoEsperado(separacao.getResultadosFormatados());
        }

        removerDuplicacaoResultadoNoFinalDosPassos(item);
    }

    private static final Pattern PONTUACAO_FINAL = Pattern.compile("[.!?;:]+$");

    /**
     * FASE15-BUG-003A: remove, de forma determinística (sem NLP/semântica),
     * uma repetição EXATA do texto de Resultado Esperado colada ao final de
     * Passos (defeito residual observado na Sessão 5 — ex.: "...Então X. X.").
     * Tolera apenas diferença de pontuação terminal/whitespace. Nunca remove
     * se isso apagaria o passo inteiro (scriptTeste == resultadoEsperado).
     * Loop defensivo cobre o caso de duplicação múltipla introduzida por este
     * próprio agente ao recombinar script+resultado antes de re-separar.
     */
    private void removerDuplicacaoResultadoNoFinalDosPassos(CenarioItem item) {
        String resultado = item.getResultadoEsperado();
        String script = item.getScriptTeste();
        if (resultado == null || resultado.isBlank() || script == null || script.isBlank()) {
            return;
        }

        String resultadoNucleo = PONTUACAO_FINAL.matcher(resultado.trim()).replaceAll("");
        if (resultadoNucleo.isBlank()) {
            return;
        }

        Pattern duplicacaoNoFinal = Pattern.compile(
                "\\s*" + Pattern.quote(resultadoNucleo) + "[.!?;:]*\\s*$");

        String scriptAtual = script;
        while (true) {
            String scriptNucleo = PONTUACAO_FINAL.matcher(scriptAtual.trim()).replaceAll("");
            if (scriptNucleo.equals(resultadoNucleo)) {
                // scriptTeste inteiro é igual ao resultado - não remove (apagaria o passo inteiro).
                break;
            }
            Matcher matcher = duplicacaoNoFinal.matcher(scriptAtual);
            if (!matcher.find()) {
                break;
            }
            String semDuplicacao = scriptAtual.substring(0, matcher.start()).trim();
            if (semDuplicacao.isBlank()) {
                break;
            }
            scriptAtual = semDuplicacao;
        }

        if (!scriptAtual.equals(script)) {
            item.setScriptTeste(scriptAtual);
        }
    }

    private String combinarTextos(String script, String resultado) {
        StringBuilder combinado = new StringBuilder();
        
        if (script != null && !script.isBlank()) {
            combinado.append(script);
        }
        
        if (resultado != null && !resultado.isBlank()) {
            if (combinado.length() > 0) {
                combinado.append("\n");
            }
            combinado.append(resultado);
        }
        
        return combinado.toString();
    }
    
    private SeparacaoBdd separarTextoBdd(String texto) {
        SeparacaoBdd separacao = new SeparacaoBdd();
        
        // Normalizar texto
        String normalizado = normalizarTexto(texto);
        
        // Dividir por keywords BDD
        String[] linhas = normalizado.split("\n");
        
        for (String linha : linhas) {
            String trimmed = linha.trim();
            
            if (trimmed.isEmpty()) {
                continue;
            }
            
            // Verificar qual keyword está presente
            if (matchKeyword(trimmed, "Dado que", "Dado")) {
                separacao.adicionarPasso(trimmed);
            } else if (matchKeyword(trimmed, "Quando")) {
                separacao.adicionarPasso(trimmed);
            } else if (matchKeyword(trimmed, "Então")) {
                separacao.adicionarResultado(trimmed);
            } else if (matchKeyword(trimmed, "E")) {
                // "E" vai para passos ou resultados dependendo do contexto
                if (separacao.emResultados()) {
                    separacao.adicionarResultado(trimmed);
                } else {
                    separacao.adicionarPasso(trimmed);
                }
            } else {
                // Linha sem keyword - adicionar ao contexto atual
                if (separacao.emResultados()) {
                    separacao.adicionarResultado(trimmed);
                } else {
                    separacao.adicionarPasso(trimmed);
                }
            }
        }
        
        return separacao;
    }
    
    private String normalizarTexto(String texto) {
        // FASE15-BUG-003: a IA às vezes escreve os passos como prosa corrida
        // separada por ". " (mesmo padrão observado nos antigos passos
        // numerados) em vez de uma linha por palavra-chave. Por isso a
        // fronteira de quebra também precisa reconhecer pontuação de fim de
        // frase (".", ";", ":") antes da keyword, não só letra/dígito.
        return texto
            .replaceAll("\\r", "")
            .replaceAll("\\s+", " ")
            // Quebrar antes de cada keyword BDD
            .replaceAll("([a-zA-ZÀ-ÿ0-9>.;:])\\s+(Dado que)", "$1\n$2")
            .replaceAll("([a-zA-ZÀ-ÿ0-9>.;:])\\s+(Dado)", "$1\n$2")
            .replaceAll("([a-zA-ZÀ-ÿ0-9>.;:])\\s+(Quando)", "$1\n$2")
            .replaceAll("([a-zA-ZÀ-ÿ0-9>.;:])\\s+(Então)", "$1\n$2")
            .replaceAll("([a-zA-ZÀ-ÿ0-9>.;:])\\s+(E)\\s+", "$1\n$2 ")
            // Limpar espaços extras
            .replaceAll("\\n{2,}", "\n")
            .trim();
    }
    
    private boolean matchKeyword(String linha, String... keywords) {
        String lower = linha.toLowerCase();
        for (String keyword : keywords) {
            if (lower.startsWith(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    private static class SeparacaoBdd {
        private final StringBuilder passos = new StringBuilder();
        private final StringBuilder resultados = new StringBuilder();
        private boolean emResultadosFlag = false;
        
        public void adicionarPasso(String linha) {
            if (passos.length() > 0) {
                passos.append("\n");
            }
            passos.append(linha);
        }
        
        public void adicionarResultado(String linha) {
            emResultadosFlag = true;
            if (resultados.length() > 0) {
                resultados.append("\n");
            }
            resultados.append(linha);
        }
        
        public boolean emResultados() {
            return emResultadosFlag;
        }
        
        public boolean temPassos() {
            return passos.length() > 0;
        }
        
        public boolean temResultados() {
            return resultados.length() > 0;
        }
        
        public String getPassosFormatados() {
            return passos.toString().trim();
        }
        
        public String getResultadosFormatados() {
            return resultados.toString().trim();
        }
    }
}
