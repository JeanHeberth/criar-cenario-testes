package com.br.criarcenariotestes.business.autoqa.knowledge;

public class ProjectKnowledgeException extends RuntimeException {
    public ProjectKnowledgeException(String message) {
        super(message);
    }

    public ProjectKnowledgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
