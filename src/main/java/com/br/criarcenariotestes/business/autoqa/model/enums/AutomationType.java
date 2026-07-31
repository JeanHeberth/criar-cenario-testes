package com.br.criarcenariotestes.business.autoqa.model.enums;

public enum AutomationType {

    WEB("Automação Web"),
    API("Automação de API"),
    MOBILE("Automação Mobile"),
    DESKTOP("Automação Desktop");

    private final String descricao;

    AutomationType(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
