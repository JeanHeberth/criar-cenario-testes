package com.br.criarcenariotestes.business.autoqa.model.enums;

public enum AutomationFramework {

    PLAYWRIGHT("Playwright"),
    CYPRESS("Cypress"),
    SELENIUM("Selenium"),
    SELENIDE("Selenide"),
    ROBOT_FRAMEWORK("Robot Framework"),
    REST_ASSURED("RestAssured"),
    UNKNOWN("Desconhecido");

    private final String descricao;

    AutomationFramework(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
