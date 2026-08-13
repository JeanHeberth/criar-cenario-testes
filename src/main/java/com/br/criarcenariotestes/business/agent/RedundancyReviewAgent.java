package com.br.criarcenariotestes.business.agent;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.parser.CenarioTextoParser;
import com.br.criarcenariotestes.business.validation.GeneratedScenariosValidator;
import com.br.criarcenariotestes.business.workflow.WorkflowContext;
import com.br.criarcenariotestes.business.workflow.WorkflowType;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedundancyReviewAgent implements BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(RedundancyReviewAgent.class);

    private final AiProviderResolver aiProviderResolver;
    private final CenarioTextoParser cenarioTextoParser;
    private final GeneratedScenariosValidator generatedScenariosValidator;

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

            GeneratedScenariosValidator.ValidationResult validacao =
                    generatedScenariosValidator.validarRespostaBruta(respostaIa);

            if (!validacao.valido()) {
                log.warn("Revisão retornou conteúdo estruturalmente inválido ('{}'). " +
                        "Mantendo cenários originais (fail closed).", validacao.motivo());
                context.setCenariosRevisados(context.getCenarios());
                return;
            }

            List<CenarioItem> cenariosRevisados = cenarioTextoParser.parsear(respostaIa);

            if (cenariosRevisados.isEmpty()) {
                log.warn("Revisão não extraiu nenhum cenário válido. Mantendo cenários originais (fail closed).");
                context.setCenariosRevisados(context.getCenarios());
                return;
            }

            List<CenarioItem> cenariosValidos = new ArrayList<>();
            for (CenarioItem item : cenariosRevisados) {
                if (generatedScenariosValidator.pareceConteudoNaoCenario(item)) {
                    log.info("Item pós-revisão descartado por não ser um cenário real " +
                            "(Passos e Resultado Esperado vazios). nome='{}'", item.getNome());
                    continue;
                }

                GeneratedScenariosValidator.ValidationResult bdd =
                        generatedScenariosValidator.validarEstruturaBdd(item.getScriptTeste());
                if (!bdd.valido()) {
                    log.warn("Cenário pós-revisão estruturalmente corrompido ('{}'). " +
                            "Fail closed: mantendo cenários originais.", bdd.motivo());
                    context.setCenariosRevisados(context.getCenarios());
                    return;
                }

                cenariosValidos.add(item);
            }

            if (cenariosValidos.isEmpty()) {
                log.warn("Nenhum cenário real restou após a validação pós-revisão. " +
                        "Mantendo cenários originais (fail closed).");
                context.setCenariosRevisados(context.getCenarios());
                return;
            }

            context.setCenariosRevisados(cenariosValidos);
            context.addMetadata("revisao_provider", provider.getName());
            context.addMetadata("cenarios_originais", context.getCenarios().size());
            context.addMetadata("cenarios_revisados", cenariosValidos.size());

            log.info("Revisão concluída. provider='{}', original={}, revisados={}",
                provider.getName(),
                context.getCenarios().size(),
                cenariosValidos.size());
            
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

            REGRA OBRIGATÓRIA DE FORMATO (FASE15-BUG-003):
            - O campo Passos recebido está em BDD/Gherkin (Dado/Dado que, E, Quando, Então).
              Você DEVE preservar essa estrutura na saída. NUNCA converta Passos de volta
              para lista numerada (1., 2., 3.) ou para texto corrido sem as palavras-chave.
            - Cada campo aparece uma única vez. Passos NÃO deve incorporar o texto de
              Resultado Esperado, Tipo, Prioridade ou Tags — essas informações continuam
              em seus próprios campos, nunca coladas ao final do último passo.
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
