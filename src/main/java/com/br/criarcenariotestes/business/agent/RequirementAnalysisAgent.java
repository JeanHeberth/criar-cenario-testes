package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequirementAnalysisAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(RequirementAnalysisAgent.class);
    
    private final AiProviderResolver aiProviderResolver;

    @Override
    public void executar(WorkflowContext context) {
        log.info("Iniciando análise de requisitos. titulo='{}'", context.getRequest().titulo());
        
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(context);
        
        try {
            AiProvider provider = aiProviderResolver.getActiveProvider();
            String requisitos = provider.gerarResposta(systemPrompt, userPrompt);
            
            context.setRequisitos(requisitos);
            context.addMetadata("requisitos_provider", provider.getName());
            
            log.info("Requisitos extraídos com sucesso. provider='{}', length={}", 
                provider.getName(), requisitos.length());
            
        } catch (Exception e) {
            log.error("Erro ao extrair requisitos: {}", e.getMessage(), e);
            context.setRequisitos("Erro ao analisar requisitos: " + e.getMessage());
        }
    }

    @Override
    public String getNome() {
        return "Requirement Analyst";
    }
    
    private String buildSystemPrompt() {
        return """
            Você é um analista de requisitos especializado em QA.
            Sua função é extrair requisitos funcionais e não-funcionais de forma clara e estruturada.
            
            Separe em:
            - Requisitos Funcionais (RF)
            - Requisitos Não-Funcionais (RNF)
            - Regras de Negócio (RN)
            - Pontos de Atenção
            
            Seja objetivo e técnico.
            """;
    }
    
    private String buildUserPrompt(WorkflowContext context) {
        return String.format("""
            Analise a seguinte história de usuário e extraia os requisitos:
            
            Título: %s
            
            Regra de Negócio:
            %s
            
            Liste os requisitos de forma estruturada.
            """,
            context.getRequest().titulo(),
            context.getRequest().regraDeNegocio()
        );
    }
}
