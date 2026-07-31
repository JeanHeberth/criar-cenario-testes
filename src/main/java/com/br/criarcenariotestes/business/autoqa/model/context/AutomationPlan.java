package com.br.criarcenariotestes.business.autoqa.model.context;

import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Plano de automação produzido pelo AutomationPlannerAgent via IA.
 * Contém apenas informações estruturais — nenhuma linha de código.
 * Deve ser aprovado pelo usuário antes da geração de código.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationPlan {

    private String testName;

    private String objective;

    private AutomationFramework framework;

    private AutomationLanguage language;

    private List<String> preconditions;

    private List<String> requiredData;

    /** Componentes (Page Objects, helpers) existentes que serão reutilizados. */
    private List<String> existingComponentsToReuse;

    /** Classes existentes que serão utilizadas. */
    private List<String> existingClassesToUse;

    /** Métodos existentes que serão utilizados. */
    private List<String> existingMethodsToUse;

    /** Arquivos novos que serão criados. */
    private List<String> filesToCreate;

    /** Arquivos existentes que poderão ser atualizados. */
    private List<String> filesToUpdate;

    private List<String> assertions;

    private List<String> risks;

    private List<String> pendingItems;

    private List<String> missingElements;

    /** True quando a IA detecta que é necessário criar um novo Page Object. */
    private boolean requiresNewPageObject;

    /** True quando o usuário deve intervir antes da geração. */
    private boolean requiresUserIntervention;

    /**
     * True quando faltam informações essenciais para gerar o teste.
     * O workflow não avança para geração quando blocked=true.
     */
    private boolean blocked;

    private String blockedReason;
}
