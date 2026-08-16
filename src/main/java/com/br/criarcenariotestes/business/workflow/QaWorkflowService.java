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

import java.util.List;

@Service
@RequiredArgsConstructor
public class QaWorkflowService {

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
        List<CenarioItem> cenariosFinais = context.getCenariosFinais();
        validarCenariosFinaisAntesDePersistir(cenariosFinais);

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
     * FASE15-BUG-003A: última barreira determinística antes da persistência.
     * Nenhuma chamada de IA, nenhum retry — se algo estruturalmente inválido
     * chegou até aqui (falha do Reviewer não capturada, item corrompido),
     * falha explicitamente em vez de persistir e reportar falso sucesso.
     */
    private void validarCenariosFinaisAntesDePersistir(List<CenarioItem> cenarios) {
        if (cenarios == null || cenarios.isEmpty()) {
            throw new ValidacaoEstruturalException("Nenhum cenário válido para persistir.");
        }

        for (CenarioItem item : cenarios) {
            GeneratedScenariosValidator.ValidationResult resultado =
                    generatedScenariosValidator.validarRepresentacaoFinal(item);
            if (!resultado.valido()) {
                throw new ValidacaoEstruturalException("Validação final antes da persistência falhou: " + resultado.motivo());
            }
        }
    }
}
