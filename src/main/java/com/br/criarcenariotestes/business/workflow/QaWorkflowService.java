package com.br.criarcenariotestes.business.workflow;

import com.br.criarcenariotestes.business.agent.*;
import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.dto.CenarioResponse;
import com.br.criarcenariotestes.business.service.AgentLoaderService;
import com.br.criarcenariotestes.business.validation.GeneratedScenariosValidator;
import com.br.criarcenariotestes.business.validation.ValidacaoEstruturalException;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QaWorkflowService {

    /**
     * Status já usado por corrigirSourceInexistente para o mesmo fim: o item
     * fica visível e utilizável, mas sinalizado para olho humano.
     *
     * <p>O veredito WAIVED (aceite formal de um item em revisão) ainda NÃO
     * existe: exigiria identidade do aprovador e data de expiração, que o
     * sistema não modela hoje. Enquanto isso, REVIEW_REQUIRED é o estado final
     * de quem foi rebaixado.
     */
    private static final String STATUS_REVISAO_NECESSARIA = "REVIEW_REQUIRED";

    private static final Logger log = LoggerFactory.getLogger(QaWorkflowService.class);

    private final RequirementAnalysisAgent requirementAnalysisAgent;
    private final TranscriptAnalysisAgent transcriptAnalysisAgent;
    private final TestPlanAgent testPlanAgent;
    private final TestScenarioAgent testScenarioAgent;
    private final RedundancyReviewAgent redundancyReviewAgent;
    private final BddFormatterAgent bddFormatterAgent;
    private final ZephyrFormatterAgent zephyrFormatterAgent;
    private final ZephyrPublisherAgent zephyrPublisherAgent;

    private final AgentLoaderService agentLoaderService;
    private final CenarioRepository cenarioRepository;
    private final GeneratedScenariosValidator generatedScenariosValidator;

    public CenarioResponse executarWorkflow(CenarioRequest request) {
        WorkflowType workflowType = request.workflowType() != null ? 
            request.workflowType() : WorkflowType.COMPLETO;
        return executarWorkflow(request, workflowType);
    }

    public CenarioResponse executarWorkflow(CenarioRequest request, WorkflowType workflowType) {
        log.info("Iniciando workflow BMAD. titulo='{}', tipo={}", 
            request.titulo(), workflowType);
        
        WorkflowContext context = new WorkflowContext(request, workflowType);
        
        String agentInstructions = agentLoaderService.loadAgentInstructions(request.agent());
        context.setAgentInstructions(agentInstructions);
        
        List<BaseAgent> agents = montarPipelineAgentes(workflowType);
        
        for (BaseAgent agent : agents) {
            if (agent.isEnabled(context)) {
                try {
                    log.info("Executando agente: {}", agent.getNome());
                    agent.executar(context);
                    log.info("Agente {} concluído com sucesso", agent.getNome());
                } catch (ValidacaoEstruturalException e) {
                    // FASE15-BUG-003A: não reenvelopar - precisa propagar identificável
                    // como falha estrutural (nunca deve ser mascarada por fallback).
                    log.error("Falha estrutural no agente {}: {}", agent.getNome(), e.getMessage());
                    throw e;
                } catch (Exception e) {
                    log.error("Erro no agente {}: {}", agent.getNome(), e.getMessage(), e);
                    throw new RuntimeException("Falha no workflow no agente: " + agent.getNome(), e);
                }
            } else {
                log.info("Agente {} desabilitado para workflow {}", agent.getNome(), workflowType);
            }
        }
        
        return salvarResultado(context);
    }

    private List<BaseAgent> montarPipelineAgentes(WorkflowType workflowType) {
        return switch (workflowType) {
            case COMPLETO -> List.of(
                requirementAnalysisAgent,
                transcriptAnalysisAgent,
                testPlanAgent,
                testScenarioAgent,
                redundancyReviewAgent,
                bddFormatterAgent,
                zephyrFormatterAgent
            );
            
            case RAPIDO -> List.of(
                requirementAnalysisAgent,
                testPlanAgent,
                testScenarioAgent,
                bddFormatterAgent,
                zephyrFormatterAgent
            );
            
            case REVISAO -> List.of(
                redundancyReviewAgent,
                bddFormatterAgent,
                zephyrFormatterAgent
            );
            
            case REGRESSAO -> List.of(
                requirementAnalysisAgent,
                testPlanAgent,
                testScenarioAgent,
                bddFormatterAgent,
                zephyrFormatterAgent
            );
        };
    }

    private CenarioResponse salvarResultado(WorkflowContext context) {
        SegregacaoFinal segregacao = segregarCenariosFinaisAntesDePersistir(context.getCenariosFinais());
        List<CenarioItem> cenariosFinais = segregacao.mantidos();
        // Escreve de volta ANTES do ZephyrPublisher: um cenário descartado
        // aqui não pode virar caso de teste real no board do Zephyr.
        context.substituirCenariosFinais(cenariosFinais);
        segregacao.registrarEm(context);

        // Publica no Zephyr só DEPOIS da validação estrutural passar - nunca
        // antes. Publicar antes (como no pipeline genérico de agentes) cria
        // casos de teste reais e permanentes no Zephyr para cenários que
        // podem ser rejeitados logo em seguida e nunca chegar a ser
        // persistidos aqui, virando lixo órfão no board do Zephyr.
        if (zephyrPublisherAgent.isEnabled(context)) {
            zephyrPublisherAgent.executar(context);
        }

        Cenario cenario = new Cenario();
        cenario.setTitulo(context.getRequest().titulo());
        cenario.setRegraDeNegocio(context.getRequest().regraDeNegocio());
        cenario.setCriteriosAceitacao(context.getCriteriosAceitacao());
        cenario.setCenarios(cenariosFinais);

        Cenario salvo = cenarioRepository.save(cenario);
        
        log.info("Workflow concluído e salvo. id='{}', cenarios={}, metadados={}", 
            salvo.getId(), 
            salvo.getCenarios() != null ? salvo.getCenarios().size() : 0,
            context.getMetadados());
        
        return new CenarioResponse(
            salvo.getId(),
            salvo.getTitulo(),
            salvo.getRegraDeNegocio(),
            salvo.getCriteriosAceitacao(),
            salvo.getCenarios()
        );
    }

    /**
     * Última barreira determinística antes da persistência. Nenhuma chamada de
     * IA, nenhum retry.
     *
     * <p>Antes isto era tudo-ou-nada: o primeiro item reprovado derrubava o
     * lote inteiro. Um cenário íntegro cuja única falha era uma quebra de linha
     * ausente descartava os outros treze — e nesta etapa não há recuperação
     * possível, o Formatter já rodou e os agentes já consumiram os tokens.
     * Descartar ali era perda pura.
     *
     * <p>A regra agora distingue o que é irrecuperável do que é apenas
     * malformado, seguindo o padrão que já existia em
     * {@code GeneratedScenariosValidator#corrigirSourceInexistente}: degradar o
     * item, não matar o lote.
     *
     * <ul>
     *   <li>sem nome, sem Passos ou sem Resultado — inutilizável: DESCARTA o item</li>
     *   <li>com conteúdo, faltando Dado/Quando — utilizável, só mal formatado:
     *       MANTÉM com status REVIEW_REQUIRED</li>
     *   <li>nenhum sobrevivente — aí sim falha, como antes</li>
     * </ul>
     *
     * <p>Nada é perdido em silêncio: o que foi descartado ou rebaixado vai para
     * o log e para os metadados da execução. Lacuna silenciosa numa ferramenta
     * de QA é pior que falha barulhenta — o usuário acharia que tem cobertura
     * completa.
     */
    private SegregacaoFinal segregarCenariosFinaisAntesDePersistir(List<CenarioItem> cenarios) {
        if (cenarios == null || cenarios.isEmpty()) {
            throw new ValidacaoEstruturalException("Nenhum cenário válido para persistir.");
        }

        List<CenarioItem> mantidos = new ArrayList<>();
        List<String> descartados = new ArrayList<>();
        List<String> rebaixados = new ArrayList<>();

        for (CenarioItem item : cenarios) {
            GeneratedScenariosValidator.ValidationResult resultado =
                    generatedScenariosValidator.validarRepresentacaoFinal(item);

            if (resultado.valido()) {
                mantidos.add(item);
                continue;
            }

            // Sem o texto reprovado, o log só diria QUE falhou, não POR QUE — e
            // como o conteúdo vem da IA e muda a cada geração, reproduzir para
            // diagnosticar custa uma rodada inteira de chamadas.
            if (generatedScenariosValidator.temConteudoAproveitavel(item)) {
                item.setStatus(STATUS_REVISAO_NECESSARIA);
                mantidos.add(item);
                rebaixados.add(item.getNome() + " — " + resultado.motivo());
                log.warn("Cenário rebaixado para {} na validação final. nome='{}', motivo='{}'. scriptTeste=<<<{}>>> resultadoEsperado=<<<{}>>>",
                        STATUS_REVISAO_NECESSARIA, item.getNome(), resultado.motivo(),
                        item.getScriptTeste(), item.getResultadoEsperado());
            } else {
                descartados.add(nomeOuAnonimo(item) + " — " + resultado.motivo());
                log.error("Cenário DESCARTADO na validação final (sem conteúdo aproveitável). nome='{}', motivo='{}'. scriptTeste=<<<{}>>> resultadoEsperado=<<<{}>>>",
                        nomeOuAnonimo(item), resultado.motivo(),
                        item.getScriptTeste(), item.getResultadoEsperado());
            }
        }

        if (mantidos.isEmpty()) {
            throw new ValidacaoEstruturalException(
                    "Validação final antes da persistência falhou: nenhum dos " + cenarios.size()
                            + " cenários tem conteúdo aproveitável. " + String.join(" | ", descartados));
        }

        return new SegregacaoFinal(mantidos, descartados, rebaixados);
    }

    private String nomeOuAnonimo(CenarioItem item) {
        return item != null && item.getNome() != null && !item.getNome().isBlank()
                ? item.getNome()
                : "(sem nome)";
    }

    /**
     * Resultado da segregação. {@code rebaixados} e {@code descartados} trazem
     * o motivo por item para que a origem apareça na resposta, e não só no log.
     */
    private record SegregacaoFinal(List<CenarioItem> mantidos,
                                   List<String> descartados,
                                   List<String> rebaixados) {

        void registrarEm(WorkflowContext context) {
            context.addMetadata("cenariosPersistidos", mantidos.size());
            if (!rebaixados.isEmpty()) {
                context.addMetadata("cenariosEmRevisao", rebaixados);
            }
            if (!descartados.isEmpty()) {
                context.addMetadata("cenariosDescartados", descartados);
            }
        }
    }
}
