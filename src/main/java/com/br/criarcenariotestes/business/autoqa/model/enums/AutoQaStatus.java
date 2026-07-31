package com.br.criarcenariotestes.business.autoqa.model.enums;

public enum AutoQaStatus {

    CREATED("Criado"),
    DISCOVERING_PROJECT("Descobrindo projeto"),
    PROJECT_DISCOVERED("Projeto descoberto"),
    ANALYZING_PROJECT("Analisando projeto"),
    PROJECT_ANALYZED("Projeto analisado"),
    PLANNING("Planejando automação"),
    PLAN_READY("Plano pronto — aguardando aprovação"),
    GENERATING("Gerando código"),
    CODE_GENERATED("Código gerado"),
    REVIEWING("Revisando código"),
    REVIEW_APPROVED("Revisão aprovada"),
    REVIEW_REJECTED("Revisão reprovada"),
    WAITING_USER_APPROVAL("Aguardando aprovação do usuário"),
    APPLYING_FILES("Aplicando arquivos"),
    EXECUTING("Executando teste"),
    EXECUTION_SUCCESS("Execução bem-sucedida"),
    EXECUTION_FAILED("Execução falhou"),
    ANALYZING_FAILURE("Analisando falha"),
    FINISHED("Finalizado"),
    ERROR("Erro");

    private final String descricao;

    AutoQaStatus(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
