package com.br.criarcenariotestes.business.workflow;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class WorkflowContext {
    
    private final CenarioRequest request;
    
    private WorkflowType workflowType;
    
    private String agentInstructions;
    
    private String requisitos;
    
    private String decisoesReuniao;
    
    private String planoMacro;
    
    private List<CenarioItem> cenarios;
    
    private List<CenarioItem> cenariosRevisados;
    
    private String criteriosAceitacao;
    
    private String formatoFinal;
    
    private Map<String, Object> metadados;
    
    public WorkflowContext(CenarioRequest request) {
        this.request = request;
        this.workflowType = WorkflowType.COMPLETO;
        this.metadados = new HashMap<>();
    }
    
    public WorkflowContext(CenarioRequest request, WorkflowType workflowType) {
        this.request = request;
        this.workflowType = workflowType;
        this.metadados = new HashMap<>();
    }
    
    public void addMetadata(String key, Object value) {
        if (this.metadados == null) {
            this.metadados = new HashMap<>();
        }
        this.metadados.put(key, value);
    }
    
    public Object getMetadata(String key) {
        if (this.metadados == null) {
            return null;
        }
        return this.metadados.get(key);
    }
    
    public List<CenarioItem> getCenariosFinais() {
        if (cenariosRevisados != null && !cenariosRevisados.isEmpty()) {
            return cenariosRevisados;
        }
        return cenarios;
    }

    /**
     * Escreve de volta no MESMO campo de onde {@link #getCenariosFinais()}
     * leria. Existe para que a segregação pré-persistência valha também para
     * quem consome o contexto depois dela — em especial o ZephyrPublisher,
     * que não pode publicar um cenário que acabou de ser descartado.
     */
    public void substituirCenariosFinais(List<CenarioItem> novos) {
        if (cenariosRevisados != null && !cenariosRevisados.isEmpty()) {
            this.cenariosRevisados = novos;
        } else {
            this.cenarios = novos;
        }
    }
}
