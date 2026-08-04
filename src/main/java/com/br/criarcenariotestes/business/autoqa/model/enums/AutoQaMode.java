package com.br.criarcenariotestes.business.autoqa.model.enums;

public enum AutoQaMode {

    PLAN_ONLY("Apenas planejamento — não gera código"),
    GENERATE_FOR_REVIEW("Gera código para revisão — aguarda aprovação antes de aplicar"),
    GENERATE_AND_EXECUTE("Gera, aplica e executa — requer aprovação explícita em cada etapa");

    private final String descricao;

    AutoQaMode(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
