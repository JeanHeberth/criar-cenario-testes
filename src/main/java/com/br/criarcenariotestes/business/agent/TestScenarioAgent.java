package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.parser.CenarioTextoParser;
import com.br.criarcenariotestes.business.validation.GeneratedScenariosValidator;
import com.br.criarcenariotestes.business.validation.ValidacaoEstruturalException;
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

    /**
     * FASE15-BUG-006: o gerador produz tipicamente 17-19 cenários em BDD
     * completo (mais verboso que o antigo formato numerado), o que esbarrava
     * no limite padrão de 4000 tokens de saída e truncava a resposta no meio
     * do último cenário. Só este agente usa um teto maior — os demais
     * continuam com o limite configurado padrão (4000).
     */
    public static final int GENERATOR_MAX_TOKENS = 8000;

    private final AiProviderResolver aiProviderResolver;
    private final CenarioTextoParser cenarioTextoParser;
    private final GeneratedScenariosValidator generatedScenariosValidator;

    @Override
    public void executar(WorkflowContext context) {
        log.info("Iniciando geração de cenários de teste. titulo='{}'", context.getRequest().titulo());

        String systemPrompt = buildSystemPrompt(context);
        String userPrompt = buildUserPrompt(context);

        try {
            AiProvider provider = aiProviderResolver.getActiveProvider();

            String respostaIa = provider.gerarResposta(systemPrompt, userPrompt, GENERATOR_MAX_TOKENS);
            List<CenarioItem> cenarios = cenarioTextoParser.parsear(respostaIa);
            GeneratedScenariosValidator.ValidationResult validacao =
                    generatedScenariosValidator.validarGeracao(respostaIa, cenarios);

            if (!validacao.valido()) {
                log.warn("Resposta da IA estruturalmente inválida ('{}'). Executando retry único.",
                        validacao.motivo());

                String retryUserPrompt = buildRetryUserPrompt(userPrompt);
                respostaIa = provider.gerarResposta(systemPrompt, retryUserPrompt, GENERATOR_MAX_TOKENS);
                cenarios = cenarioTextoParser.parsear(respostaIa);
                validacao = generatedScenariosValidator.validarGeracao(respostaIa, cenarios);

                if (!validacao.valido()) {
                    log.error("Falha na geração de cenários mesmo após retry único. motivo='{}'",
                            validacao.motivo());
                    throw new ValidacaoEstruturalException(
                            "Falha na geração de cenários: resposta da IA inválida após retry - " + validacao.motivo());
                }
            }

            String criterios = cenarioTextoParser.extrairCriterios(respostaIa);

            context.setCenarios(cenarios);
            context.setCriteriosAceitacao(criterios);
            context.addMetadata("cenarios_provider", provider.getName());
            context.addMetadata("cenarios_count", cenarios.size());

            log.info("Cenários gerados com sucesso. provider='{}', quantidade={}",
                provider.getName(), cenarios.size());

        } catch (ValidacaoEstruturalException e) {
            // FASE15-BUG-003A: não reenvelopar - precisa propagar identificável
            // como falha estrutural (nunca deve ser mascarada por fallback).
            throw e;
        } catch (Exception e) {
            log.error("Erro ao gerar cenários: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na geração de cenários", e);
        }
    }

    private String buildRetryUserPrompt(String userPrompt) {
        return userPrompt + """


            ATENÇÃO: sua resposta anterior não continha cenários de teste válidos —
            ela veio como plano de arquivos, texto vazio, fora do formato esperado,
            ou com o campo Passos fora do contrato obrigatório de BDD/Gherkin.
            Não existe etapa de aprovação nesta chamada. Responda AGORA diretamente
            com os cenários de teste completos, sem plano de arquivos, sem pedido de
            confirmação e sem template vazio. O campo Passos DEVE usar Dado/Dado que,
            Quando e Então (com E quando necessário), cada palavra-chave em sua
            própria linha — nunca passos numerados (1., 2., 3.).
            """;
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
            Dado [contexto/pré-condição relevante]
            E [contexto adicional, quando necessário]
            Quando [ação/evento]
            E [ação adicional, quando necessária]
            Então [resultado verificável]
            E [resultado adicional, quando necessário]
            Resultado esperado: [resultado]
            ---

            Passos deve ser escrito em BDD/Gherkin (Dado/Dado que, E, Quando, Então),
            nunca como lista numerada. Todo cenário precisa ter ao menos um Dado,
            um Quando e um Então, cada um em sua própria linha.
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
