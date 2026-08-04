package com.br.criarcenariotestes.business.autoqa.scenario;

public class ScenarioAnalysisException extends RuntimeException {
    public ScenarioAnalysisException(String message) {
        super(message);
    }

    public ScenarioAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
