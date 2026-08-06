package com.br.criarcenariotestes.business.autoqa.model.learning;

/**
 * TEAM e GLOBAL são valores reservados: nesta fase nenhuma classe de produção
 * (collector, IA, service ou builder) pode atribuí-los a um LearningItem —
 * LearningValidator rejeita qualquer item com esses dois escopos.
 */
public enum LearningScope {
    EXECUTION,
    PROJECT,
    FRAMEWORK,
    TEAM,
    GLOBAL
}
