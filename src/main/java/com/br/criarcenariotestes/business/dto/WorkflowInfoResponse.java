package com.br.criarcenariotestes.business.dto;

import com.br.criarcenariotestes.business.workflow.WorkflowType;

public record WorkflowInfoResponse(
        WorkflowType tipo,
        String descricao,
        int quantidadeAgentes,
        String tempoEstimado,
        String[] agentes
) {
    public static WorkflowInfoResponse from(WorkflowType tipo) {
        return switch (tipo) {
            case COMPLETO -> new WorkflowInfoResponse(
                    WorkflowType.COMPLETO,
                    "Workflow completo com todos os agentes para máxima qualidade",
                    6,
                    "3-5 minutos",
                    new String[]{
                            "Requirement Analyst",
                            "Transcript Analyst",
                            "Test Planning Agent",
                            "Test Scenario Generator",
                            "Redundancy Reviewer",
                            "Zephyr Formatter"
                    }
            );
            case RAPIDO -> new WorkflowInfoResponse(
                    WorkflowType.RAPIDO,
                    "Workflow rápido sem análise de transcrições e revisão de redundâncias",
                    4,
                    "1-2 minutos",
                    new String[]{
                            "Requirement Analyst",
                            "Test Planning Agent",
                            "Test Scenario Generator",
                            "Zephyr Formatter"
                    }
            );
            case REVISAO -> new WorkflowInfoResponse(
                    WorkflowType.REVISAO,
                    "Apenas revisão de cenários existentes",
                    2,
                    "30-60 segundos",
                    new String[]{
                            "Redundancy Reviewer",
                            "Zephyr Formatter"
                    }
            );
            case REGRESSAO -> new WorkflowInfoResponse(
                    WorkflowType.REGRESSAO,
                    "Análise de impacto para testes de regressão",
                    4,
                    "2-3 minutos",
                    new String[]{
                            "Requirement Analyst",
                            "Test Planning Agent",
                            "Test Scenario Generator",
                            "Zephyr Formatter"
                    }
            );
        };
    }
}
