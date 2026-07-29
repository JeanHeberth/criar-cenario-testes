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
public class TestPlanAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(TestPlanAgent.class);
    
    private final AiProviderResolver aiProviderResolver;

    @Override
    public void executar(WorkflowContext context) {
        log.info("Iniciando criação do plano de testes. titulo='{}'", context.getRequest().titulo());
        
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(context);
        
        try {
            AiProvider provider = aiProviderResolver.getActiveProvider();
            String planoMacro = provider.gerarResposta(systemPrompt, userPrompt);
            
            context.setPlanoMacro(planoMacro);
            context.addMetadata("plano_provider", provider.getName());
            
            log.info("Plano de testes criado com sucesso. provider='{}', length={}", 
                provider.getName(), planoMacro.length());
            
        } catch (Exception e) {
            log.error("Erro ao criar plano de testes: {}", e.getMessage(), e);
            context.setPlanoMacro("Erro ao gerar plano: " + e.getMessage());
        }
    }

    @Override
    public String getNome() {
        return "Test Planning Agent";
    }
    
    private String buildSystemPrompt() {
        return """
            Você é um arquiteto de testes especializado em criar planos macro de teste.
            
            Sua função é:
            - Definir estratégia de cobertura de testes
            - Identificar cenários principais (positivos, negativos, edge cases)
            - Sugerir tipos de teste (funcional, integração, regressão)
            - Priorizar cenários por risco e impacto
            
            Não gere casos de teste detalhados ainda, apenas o plano macro.
            """;
    }
    
    private String buildUserPrompt(WorkflowContext context) {
        String requisitos = context.getRequisitos() != null ? context.getRequisitos() : "";
        String decisoes = context.getDecisoesReuniao() != null ? context.getDecisoesReuniao() : "";
        
        return String.format("""
            Crie um plano macro de testes baseado no contexto abaixo:
            
            Título: %s
            
            Regra de Negócio:
            %s
            
            Requisitos:
            %s
            
            Decisões e Ambiguidades:
            %s
            
            Forneça um plano estruturado com estratégia de cobertura.
            """,
            context.getRequest().titulo(),
            context.getRequest().regraDeNegocio(),
            requisitos,
            decisoes
        );
    }
}
