package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.parser.CenarioTextoParser;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.business.workflow.WorkflowType;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RedundancyReviewAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(RedundancyReviewAgent.class);
    
    private final AiProviderResolver aiProviderResolver;
    private final CenarioTextoParser cenarioTextoParser;

    @Override
    public void executar(WorkflowContext context) {
        log.info("Iniciando revisão de redundâncias. titulo='{}'", context.getRequest().titulo());
        
        if (context.getCenarios() == null || context.getCenarios().isEmpty()) {
            log.warn("Nenhum cenário para revisar. Pulando redundancy review.");
            context.setCenariosRevisados(context.getCenarios());
            return;
        }
        
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(context);
        
        try {
            AiProvider provider = aiProviderResolver.getActiveProvider();
            String respostaIa = provider.gerarResposta(systemPrompt, userPrompt);
            
            List<CenarioItem> cenariosRevisados = cenarioTextoParser.parsear(respostaIa);
            
            context.setCenariosRevisados(cenariosRevisados);
            context.addMetadata("revisao_provider", provider.getName());
            context.addMetadata("cenarios_originais", context.getCenarios().size());
            context.addMetadata("cenarios_revisados", cenariosRevisados.size());
            
            log.info("Revisão concluída. provider='{}', original={}, revisados={}", 
                provider.getName(), 
                context.getCenarios().size(), 
                cenariosRevisados.size());
            
        } catch (Exception e) {
            log.warn("Erro ao revisar redundâncias: {}. Mantendo cenários originais.", e.getMessage());
            context.setCenariosRevisados(context.getCenarios());
        }
    }

    @Override
    public String getNome() {
        return "Redundancy Reviewer";
    }
    
    @Override
    public boolean isEnabled(WorkflowContext context) {
        WorkflowType type = context.getWorkflowType();
        return type == WorkflowType.COMPLETO || type == WorkflowType.REVISAO;
    }
    
    private String buildSystemPrompt() {
        return """
            Você é um revisor de casos de teste especializado em otimização.
            
            Sua função é:
            - Identificar cenários redundantes ou duplicados
            - Sugerir parametrização com variáveis quando aplicável
            - Consolidar cenários similares
            - Manter cobertura de testes sem perder qualidade
            
            Retorne os cenários otimizados no mesmo formato original.
            Remova apenas redundâncias reais, não sacrifique cobertura.
            """;
    }
    
    private String buildUserPrompt(WorkflowContext context) {
        StringBuilder cenariosTexto = new StringBuilder();
        
        for (CenarioItem item : context.getCenarios()) {
            cenariosTexto.append("---\n");
            cenariosTexto.append("Nome: ").append(item.getNome()).append("\n");
            if (item.getObjetivo() != null) {
                cenariosTexto.append("Objetivo: ").append(item.getObjetivo()).append("\n");
            }
            if (item.getPrecondicao() != null) {
                cenariosTexto.append("Pré-condições: ").append(item.getPrecondicao()).append("\n");
            }
            if (item.getScriptTeste() != null) {
                cenariosTexto.append("Passos:\n").append(item.getScriptTeste()).append("\n");
            }
            if (item.getResultadoEsperado() != null) {
                cenariosTexto.append("Resultado esperado: ").append(item.getResultadoEsperado()).append("\n");
            }
            cenariosTexto.append("---\n\n");
        }
        
        return String.format("""
            Revise os seguintes casos de teste e remova redundâncias:
            
            %s
            
            Retorne os cenários otimizados mantendo o mesmo formato.
            Sugira variáveis quando houver padrões repetitivos.
            """,
            cenariosTexto.toString()
        );
    }
}
