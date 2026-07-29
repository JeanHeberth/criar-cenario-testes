package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ZephyrFormatterAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(ZephyrFormatterAgent.class);

    @Override
    public void executar(WorkflowContext context) {
        log.info("Iniciando formatação para Zephyr. titulo='{}'", context.getRequest().titulo());
        
        List<CenarioItem> cenarios = context.getCenariosRevisados();
        if (cenarios == null || cenarios.isEmpty()) {
            cenarios = context.getCenarios();
        }
        
        if (cenarios == null || cenarios.isEmpty()) {
            log.warn("Nenhum cenário para formatar.");
            return;
        }
        
        try {
            String formatoZephyr = formatarParaZephyr(cenarios, context);
            
            context.setFormatoFinal(formatoZephyr);
            context.addMetadata("formato", "Zephyr");
            
            log.info("Formatação Zephyr concluída. cenarios={}", cenarios.size());
            
        } catch (Exception e) {
            log.error("Erro ao formatar para Zephyr: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getNome() {
        return "Zephyr Formatter";
    }
    
    private String formatarParaZephyr(List<CenarioItem> cenarios, WorkflowContext context) {
        StringBuilder zephyr = new StringBuilder();
        
        zephyr.append("# Casos de Teste - ").append(context.getRequest().titulo()).append("\n\n");
        zephyr.append("**Regra de Negócio:**\n");
        zephyr.append(context.getRequest().regraDeNegocio()).append("\n\n");
        
        if (context.getCriteriosAceitacao() != null && !context.getCriteriosAceitacao().isBlank()) {
            zephyr.append("**Critérios de Aceitação:**\n");
            zephyr.append(context.getCriteriosAceitacao()).append("\n\n");
        }
        
        zephyr.append("---\n\n");
        zephyr.append("## Casos de Teste\n\n");
        
        int contador = 1;
        for (CenarioItem item : cenarios) {
            zephyr.append("### CT").append(String.format("%03d", contador))
                   .append(" - ").append(item.getNome()).append("\n\n");
            
            if (item.getObjetivo() != null && !item.getObjetivo().isBlank()) {
                zephyr.append("**Objetivo:**\n");
                zephyr.append(item.getObjetivo()).append("\n\n");
            }
            
            if (item.getPrecondicao() != null && !item.getPrecondicao().isBlank()) {
                zephyr.append("**Pré-condições:**\n");
                zephyr.append(item.getPrecondicao()).append("\n\n");
            }
            
            if (item.getScriptTeste() != null && !item.getScriptTeste().isBlank()) {
                zephyr.append("**Passos:**\n");
                zephyr.append(formatarPassos(item.getScriptTeste())).append("\n\n");
            }
            
            if (item.getResultadoEsperado() != null && !item.getResultadoEsperado().isBlank()) {
                zephyr.append("**Resultado Esperado:**\n");
                zephyr.append(item.getResultadoEsperado()).append("\n\n");
            }
            
            zephyr.append("---\n\n");
            contador++;
        }
        
        return zephyr.toString();
    }
    
    private String formatarPassos(String passos) {
        if (passos == null || passos.isBlank()) {
            return "";
        }
        
        String[] linhas = passos.split("\n");
        StringBuilder formatado = new StringBuilder();
        
        for (String linha : linhas) {
            String trimmed = linha.trim();
            if (!trimmed.isEmpty()) {
                if (!trimmed.matches("^\\d+\\..*")) {
                    formatado.append("- ");
                }
                formatado.append(trimmed).append("\n");
            }
        }
        
        return formatado.toString();
    }
}
