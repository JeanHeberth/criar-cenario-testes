package com.br.criarcenariotestes.business.autoqa.model.enums;

public enum AutomationLanguage {

    TYPESCRIPT("TypeScript"),
    JAVASCRIPT("JavaScript"),
    JAVA("Java"),
    PYTHON("Python"),
    CSHARP("C#"),
    ROBOT("Robot"),
    UNKNOWN("Desconhecido");

    private final String descricao;

    AutomationLanguage(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
