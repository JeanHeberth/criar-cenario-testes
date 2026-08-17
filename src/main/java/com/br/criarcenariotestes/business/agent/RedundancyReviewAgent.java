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

                generatedScenariosValidator.corrigirSourceInexistente(
                        item, context.getRequest().regraDeNegocio(), context.getRequisitos());
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

            REGRA OBRIGATÓRIA DE STATUS EPISTÊMICO (FASE15-BUG-005) — você é a
            segunda barreira contra comportamento sem suporte documental:
            - Cada cenário recebido já vem com um Status (APPROVED ou REVIEW_REQUIRED)
              definido pelo gerador. Você DEVE preservar esse Status na saída, salvo se
              tiver certeza de que ele está errado.
            - Se, ao consolidar, você perceber que um cenário afirma comportamento de
              produto (ex.: "Então o sistema deve...") sem suporte na regra de negócio,
              nos requisitos, nas decisões ou por inferência lógica direta defensável —
              mesmo que ele tenha vindo como APPROVED — reclassifique-o: mude o Status
              para REVIEW_REQUIRED, adicione aos Rótulos "exploratorio, ponto-a-validar",
              e reescreva o Resultado Esperado/a linha "Então" para linguagem de ponto a
              validar em vez de afirmação categórica.
            - NÃO promova um cenário REVIEW_REQUIRED para APPROVED só por consolidação.
            - NÃO apague cenários exploratórios legítimos — apenas preserve ou reforce a
              classificação deles; eles continuam contando como cobertura útil.
            - NÃO invente novos requisitos, nem novo comportamento de UI/produto que não
              esteja nos cenários recebidos — sua função é consolidar e reclassificar,
              nunca adicionar conteúdo novo.

            REGRA OBRIGATÓRIA — O QUE VALIDAR vs. QUANDO/COMO VALIDAR (FASE15-BUG-005A):
            - Uma regra que define O QUE validar (obrigatoriedade, formato, quantidade
              de dígitos etc.) NÃO define automaticamente QUANDO ou COMO isso é validado.
            - Para cada cenário APPROVED, pergunte-se: a fonte define explicitamente o
              momento/interação da validação? Se o cenário afirmar comportamento como
              "tempo real", "imediatamente", "durante a digitação"/"durante o
              preenchimento", "onBlur"/"ao sair do campo", "onChange", ou "antes do
              submit" (quando a fonte só define o resultado final, não o momento da
              mensagem) SEM que a fonte defina esse timing explicitamente — reclassifique
              para REVIEW_REQUIRED, adicione "exploratorio, ponto-a-validar" aos Rótulos,
              e reescreva o Resultado Esperado em linguagem de ponto a validar.
            - Se a fonte definir o timing explicitamente (ex.: "mensagem exibida
              imediatamente ao sair do campo" está no requisito/regra), o cenário
              correspondente permanece APPROVED normalmente — a regra é sobre ausência
              de fonte para o timing, não sobre proibir esse comportamento quando
              documentado.

            REGRA OBRIGATÓRIA DE RASTREABILIDADE DE EVIDÊNCIA (FASE15-BUG-005B) —
            você é a segunda barreira, agora sobre os campos Evidência/Fontes:
            - Cada cenário recebido já vem com Evidência (DOCUMENTED, DIRECT_INFERENCE
              ou EXPLORATORY) e Fontes definidos pelo gerador. A validação estrutural
              (existência literal da fonte) já foi feita em código — sua responsabilidade
              é a validação SEMÂNTICA: a fonte citada realmente sustenta o comportamento
              afirmado no cenário, ou apenas existe no documento mas trata de outro
              assunto?
            - Exemplo: se a fonte RN-A-02 documenta apenas o formato/DDD do telefone,
              ela NÃO sustenta um cenário que afirma "ocultar o campo CEP quando o DDD
              for inválido" — mesmo citando um ID real, esse cenário não tem suporte
              semântico. Reclassifique: Evidência: EXPLORATORY, Status: REVIEW_REQUIRED,
              Fontes: Não se aplica.
            - Cenário misto (menor certeza vence): se apenas parte do cenário tem
              suporte semântico real, o cenário inteiro assume a classificação da parte
              de menor certeza (EXPLORATORY/REVIEW_REQUIRED) — nunca aprovação parcial.
            - NÃO promova Evidência de EXPLORATORY para DOCUMENTED ou DIRECT_INFERENCE
              só por consolidação — a mesma regra de não promover REVIEW_REQUIRED para
              APPROVED se aplica aqui.
            - Em caso de dúvida sobre se a fonte sustenta semanticamente o
              comportamento, prefira o lado conservador: EXPLORATORY/REVIEW_REQUIRED.
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
            if (item.getRotulos() != null && !item.getRotulos().isBlank()) {
                cenariosTexto.append("Rótulos: ").append(item.getRotulos()).append("\n");
            }
            if (item.getStatus() != null && !item.getStatus().isBlank()) {
                cenariosTexto.append("Status: ").append(item.getStatus()).append("\n");
            }
            if (item.getEvidenceType() != null && !item.getEvidenceType().isBlank()) {
                cenariosTexto.append("Evidência: ").append(item.getEvidenceType()).append("\n");
            }
            if (item.getEvidenceSources() != null && !item.getEvidenceSources().isBlank()) {
                cenariosTexto.append("Fontes: ").append(item.getEvidenceSources()).append("\n");
            }
            cenariosTexto.append("---\n\n");
        }

        String regraDeNegocio = context.getRequest().regraDeNegocio() != null
                ? context.getRequest().regraDeNegocio()
                : "";

        return String.format("""
            Revise os seguintes casos de teste e remova redundâncias:

            %s

            Contexto documental bruto disponível (use para checar semanticamente se as
            Fontes citadas nos cenários realmente sustentam o comportamento afirmado):

            %s

            Retorne os cenários otimizados mantendo o mesmo formato.
            Sugira variáveis quando houver padrões repetitivos.
            """,
            cenariosTexto.toString(),
            regraDeNegocio
        );
    }
}
