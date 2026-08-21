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
            // No Resultado a repetição é aparada mantendo UMA ocorrência: o
            // texto ainda pertence a este campo, só não pode aparecer duas
            // vezes. Nos Passos (abaixo) ela é removida por completo, porque
            // ali não é o lugar dela.
            item.setResultadoEsperado(
                    removerRepeticaoNoFinal(separacao.getResultadosFormatados(), resultadoOriginal, 1));
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
        String limpo = removerRepeticaoNoFinal(item.getScriptTeste(), item.getResultadoEsperado(), 0);
        if (limpo != null && !limpo.equals(item.getScriptTeste())) {
            item.setScriptTeste(limpo);
        }
    }

    /**
     * Apara, de forma determinística (sem NLP/semântica), repetições EXATAS de
     * {@code nucleoBruto} coladas ao FINAL de {@code texto}, tolerando apenas
     * diferença de pontuação terminal e whitespace.
     *
     * @param ocorrenciasAManter quantas cópias podem sobrar. 0 nos Passos (o
     *        Resultado não pertence àquele campo); 1 no Resultado (pertence,
     *        mas não pode duplicar). Nunca reduz o texto a vazio nem apaga a
     *        última cópia quando ela é o texto inteiro.
     */
    private String removerRepeticaoNoFinal(String texto, String nucleoBruto, int ocorrenciasAManter) {
        if (texto == null || texto.isBlank() || nucleoBruto == null || nucleoBruto.isBlank()) {
            return texto;
        }
        String nucleo = PONTUACAO_FINAL.matcher(nucleoBruto.trim()).replaceAll("");
        if (nucleo.isBlank()) {
            return texto;
        }
        Pattern noFinal = Pattern.compile("\\s*" + Pattern.quote(nucleo) + "[.!?;:]*\\s*$");

        String atual = texto;
        while (contarOcorrencias(atual, nucleo) > ocorrenciasAManter) {
            String atualNucleo = PONTUACAO_FINAL.matcher(atual.trim()).replaceAll("");
            if (atualNucleo.equals(nucleo)) {
                break;
            }
            Matcher matcher = noFinal.matcher(atual);
            if (!matcher.find()) {
                break;
            }
            String semDuplicacao = atual.substring(0, matcher.start()).trim();
            if (semDuplicacao.isBlank()) {
                break;
            }
            atual = semDuplicacao;
        }
        return atual;
    }

    private int contarOcorrencias(String texto, String trecho) {
        int total = 0;
        int idx = 0;
        while ((idx = texto.indexOf(trecho, idx)) != -1) {
            total++;
            idx += trecho.length();
        }
        return total;
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
        // A IA nem sempre escreve uma linha por palavra-chave: já foram
        // observados passos em prosa corrida separada por ". ", passos
        // numerados e dados em TABELA MARKDOWN. Por isso o texto é achatado
        // numa linha só e a quebra é reinserida antes de cada keyword.
        //
        // A fronteira é \S (qualquer não-espaço) de propósito. Antes era uma
        // classe enumerada de caracteres "permitidos" e ela foi ampliada duas
        // vezes na marra (".;:" e depois "|)]"), sempre depois de um cenário
        // íntegro ser reprovado em produção. Enumerar caracteres é uma
        // denylist: a IA sempre acha o próximo que falta — aspas de JSON no
        // passo, vírgula, "}", hífen ou a "/" de "e/ou" derrubavam todos.
        // Como o texto já foi achatado, "qualquer caractere antes do espaço"
        // é a regra correta e não tem próximo furo.
        return texto
            .replaceAll("\\r", "")
            .replaceAll("\\s+", " ")
            // Quebrar antes de cada keyword BDD
            .replaceAll("(\\S)\\s+(Dado que)", "$1\n$2")
            .replaceAll("(\\S)\\s+(Dado)", "$1\n$2")
            .replaceAll("(\\S)\\s+(Quando)", "$1\n$2")
            .replaceAll("(\\S)\\s+(Então)", "$1\n$2")
            .replaceAll("(\\S)\\s+(E)\\s+", "$1\n$2 ")
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
