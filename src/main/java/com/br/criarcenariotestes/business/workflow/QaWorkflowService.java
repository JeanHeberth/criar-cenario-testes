package com.br.criarcenariotestes.business.workflow;

import com.br.criarcenariotestes.business.agent.*;
import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.dto.CenarioResponse;
import com.br.criarcenariotestes.business.service.AgentLoaderService;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
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
    
    private final AgentLoaderService agentLoaderService;
    private final CenarioRepository cenarioRepository;

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
        Cenario cenario = new Cenario();
        cenario.setTitulo(context.getRequest().titulo());
        cenario.setRegraDeNegocio(context.getRequest().regraDeNegocio());
        cenario.setCriteriosAceitacao(context.getCriteriosAceitacao());
        cenario.setCenarios(context.getCenariosFinais());
        
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
}
