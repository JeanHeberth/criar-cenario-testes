package com.br.criarcenariotestes.business.autoqa.model.context;

import java.util.List;

/**
 * Informações de um método detectado via análise textual do código de automação.
 * Baseado em análise de texto estruturada — não usa AST nesta versão.
 */
public record MethodInfo(

        String name,

        List<MethodParameter> parameters,

        String returnType,

        boolean async,

        String visibility,

        int lineNumber,

        String sourceFile

) {

    public String signature() {
        String params = parameters == null ? "" :
                parameters.stream()
                        .map(MethodParameter::toString)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
        String asyncPrefix = async ? "async " : "";
        String ret = returnType != null ? ": " + returnType : "";
        return asyncPrefix + name + "(" + params + ")" + ret;
    }
}
