package com.br.criarcenariotestes.business.workflow;

public enum WorkflowType {
    
    COMPLETO("Workflow completo com todos os agentes"),
    
    RAPIDO("Workflow rápido sem análise de transcrições e revisão de redundâncias"),
    
    REVISAO("Apenas revisão de cenários existentes"),
    
    REGRESSAO("Análise de impacto para testes de regressão");
    
    private final String descricao;
    
    WorkflowType(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}
