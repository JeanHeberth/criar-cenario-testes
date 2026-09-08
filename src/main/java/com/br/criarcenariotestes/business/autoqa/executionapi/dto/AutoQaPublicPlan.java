package com.br.criarcenariotestes.business.autoqa.executionapi.dto;

import java.util.List;

/**
 * Visão pública do plano técnico, para o portão de aprovação deixar de ser
 * cego.
 *
 * <p>Sem ela o usuário clicava em "Aprovar e gerar código" sem ver o que seria
 * gerado, e as advertências da auditoria de reuso — que existem justamente para
 * informar essa decisão — não chegavam a ele.
 *
 * <p>É uma visão CURADA, não o TechnicalPlanResult cru: expõe o que sustenta a
 * decisão de aprovar (o que será criado, por quê, e o que a auditoria
 * questionou) e deixa de fora o detalhe interno de planejamento.
 */
public record AutoQaPublicPlan(
        String title,
        String strategy,
        List<AutoQaPublicFileAction> fileActions,
        List<AutoQaPublicPlanWarning> warnings
) {

    public record AutoQaPublicFileAction(
            String relativePath,
            String operation,
            String componentType,
            String reason
    ) {}

    public record AutoQaPublicPlanWarning(
            String code,
            String description,
            boolean requiresHumanDecision
    ) {}
}
