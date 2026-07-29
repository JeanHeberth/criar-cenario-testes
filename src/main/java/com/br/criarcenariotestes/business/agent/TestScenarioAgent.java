package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.parser.CenarioTextoParser;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TestScenarioAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(TestScenarioAgent.class);
    
    private final AiProviderResolver aiProviderResolver;
    private final CenarioTextoParser cenarioTextoParser;

    @Override
    public void executar(WorkflowContext context) {
        log.info("Iniciando geração de cenários de teste. titulo='{}'", context.getRequest().titulo());
        
        String systemPrompt = buildSystemPrompt(context);
        String userPrompt = buildUserPrompt(context);
        
        try {
            AiProvider provider = aiProviderResolver.getActiveProvider();
            String respostaIa = provider.gerarResposta(systemPrompt, userPrompt);
            
            List<CenarioItem> cenarios = cenarioTextoParser.parsear(respostaIa);
            String criterios = cenarioTextoParser.extrairCriterios(respostaIa);
            
            context.setCenarios(cenarios);
            context.setCriteriosAceitacao(criterios);
            context.addMetadata("cenarios_provider", provider.getName());
            context.addMetadata("cenarios_count", cenarios.size());
            
            log.info("Cenários gerados com sucesso. provider='{}', quantidade={}", 
                provider.getName(), cenarios.size());
            
        } catch (Exception e) {
            log.error("Erro ao gerar cenários: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na geração de cenários", e);
        }
    }

    @Override
    public String getNome() {
        return "Test Scenario Generator";
    }
    
    private String buildSystemPrompt(WorkflowContext context) {
        String basePrompt = """
            Você é um especialista em criação de casos de teste detalhados.
            
            Gere cenários de teste no formato:
            
            ---
            Nome: [nome do cenário]
            Objetivo: [objetivo]
            Pré-condições: [condições]
            Passos:
            1. [passo]
            2. [passo]
            Resultado esperado: [resultado]
            ---
            
            Inclua cenários positivos, negativos e edge cases.
            Seja específico e detalhado.
            """;
        
        String agentInstructions = context.getAgentInstructions();
        if (agentInstructions != null && !agentInstructions.isBlank()) {
            return agentInstructions + "\n\n---\n\n" + basePrompt;
        }
        
        return basePrompt;
    }
    
    private String buildUserPrompt(WorkflowContext context) {
        String requisitos = context.getRequisitos() != null ? context.getRequisitos() : "";
        String decisoes = context.getDecisoesReuniao() != null ? context.getDecisoesReuniao() : "";
        String plano = context.getPlanoMacro() != null ? context.getPlanoMacro() : "";
        
        return String.format("""
            Gere casos de teste detalhados baseados no contexto:
            
            Título: %s
            
            Regra de Negócio:
            %s
            
            Requisitos:
            %s
            
            Decisões:
            %s
            
            Plano de Testes:
            %s
            
            Gere os casos de teste completos.
            """,
            context.getRequest().titulo(),
            context.getRequest().regraDeNegocio(),
            requisitos,
            decisoes,
            plano
        );
    }
}
