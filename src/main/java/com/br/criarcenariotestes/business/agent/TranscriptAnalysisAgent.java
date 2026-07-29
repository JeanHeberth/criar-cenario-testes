package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.business.workflow.WorkflowType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TranscriptAnalysisAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(TranscriptAnalysisAgent.class);
    
    private final AiProviderResolver aiProviderResolver;

    @Override
    public void executar(WorkflowContext context) {
        log.info("Iniciando análise de transcrições. titulo='{}'", context.getRequest().titulo());
        
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(context);
        
        try {
            AiProvider provider = aiProviderResolver.getActiveProvider();
            String decisoes = provider.gerarResposta(systemPrompt, userPrompt);
            
            context.setDecisoesReuniao(decisoes);
            context.addMetadata("transcript_provider", provider.getName());
            
            log.info("Decisões extraídas com sucesso. provider='{}', length={}", 
                provider.getName(), decisoes.length());
            
        } catch (Exception e) {
            log.warn("Erro ao analisar transcrições: {}", e.getMessage());
            context.setDecisoesReuniao("Nenhuma transcrição de reunião disponível.");
        }
    }

    @Override
    public String getNome() {
        return "Transcript Analyst";
    }
    
    @Override
    public boolean isEnabled(WorkflowContext context) {
        return context.getWorkflowType() == WorkflowType.COMPLETO;
    }
    
    private String buildSystemPrompt() {
        return """
            Você é um analista de reuniões especializado em extrair decisões técnicas.
            
            Sua função é identificar:
            - Decisões tomadas pela equipe
            - Ambiguidades que precisam de esclarecimento
            - Pontos não discutidos que impactam os testes
            - Premissas assumidas
            
            Seja objetivo e destaque incertezas.
            """;
    }
    
    private String buildUserPrompt(WorkflowContext context) {
        String requisitos = context.getRequisitos() != null ? context.getRequisitos() : "";
        
        return String.format("""
            Analise o seguinte contexto e identifique decisões e ambiguidades:
            
            Título: %s
            
            Regra de Negócio:
            %s
            
            Requisitos Identificados:
            %s
            
            Liste decisões, ambiguidades e premissas.
            """,
            context.getRequest().titulo(),
            context.getRequest().regraDeNegocio(),
            requisitos
        );
    }
}
