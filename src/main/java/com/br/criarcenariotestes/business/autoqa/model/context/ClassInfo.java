package com.br.criarcenariotestes.business.autoqa.model.context;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Informações de uma classe ou interface detectada via análise textual.
 */
@Getter
@Builder
public class ClassInfo {

    private final String name;

    private final String type; // "class", "abstract class", "interface"

    private final List<MethodInfo> methods;

    private final List<String> importStatements;

    private final String sourceFile;

    public boolean isPageObject() {
        return name != null && (
                name.endsWith("Page") ||
                name.endsWith("PageObject") ||
                name.endsWith("PO")
        );
    }

    public boolean isInterface() {
        return "interface".equals(type);
    }
}
