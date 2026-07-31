package com.br.criarcenariotestes.business.autoqa.model.context;

/**
 * Parâmetro de um método detectado no código de automação.
 */
public record MethodParameter(

        String name,

        String type

) {

    @Override
    public String toString() {
        return type != null ? name + ": " + type : name;
    }
}
