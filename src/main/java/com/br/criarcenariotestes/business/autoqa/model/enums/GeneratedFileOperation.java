package com.br.criarcenariotestes.business.autoqa.model.enums;

public enum GeneratedFileOperation {

    CREATE("Criar novo arquivo"),
    UPDATE("Atualizar arquivo existente"),
    DELETE("Excluir arquivo (bloqueado na v1)");

    private final String descricao;

    GeneratedFileOperation(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
