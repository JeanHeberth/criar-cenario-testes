package com.br.criarcenariotestes.business.autoqa.model.context;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Resultado da análise textual estruturada do projeto de automação.
 * Não contém inferências de IA — apenas o que foi encontrado deterministicamente.
 */
@Getter
@Builder
public class ProjectAnalysisResult {

    /** Todas as classes encontradas no projeto. */
    private final List<ClassInfo> classes;

    /** Subconjunto das classes que parecem ser Page Objects. */
    private final List<ClassInfo> pageObjects;

    /** Caminhos relativos de arquivos de teste (.spec.ts, .cy.ts, etc.). */
    private final List<String> testFiles;

    /** Caminhos relativos de arquivos de fixture. */
    private final List<String> fixtureFiles;

    /** Caminhos relativos de arquivos helper/util. */
    private final List<String> helperFiles;

    /** Nomes de comandos customizados detectados (relevante para Cypress). */
    private final List<String> customCommands;

    /** Nomes dos describe blocks encontrados nos arquivos de teste. */
    private final List<String> describeBlocks;

    /** Nomes dos test/it blocks encontrados. */
    private final List<String> testCases;

    /** Convenções identificadas (ex: padrão de import, padrão de asserção). */
    private final List<String> conventions;

    /** Lacunas técnicas identificadas (ex: método necessário não encontrado). */
    private final List<String> gaps;

    /** Avisos não bloqueantes. */
    private final List<String> warnings;

    /** Metadados adicionais (contagens, padrões de nomenclatura, etc.). */
    private final Map<String, Object> metadata;

    private final LocalDateTime analyzedAt;

    public boolean hasPageObjects() {
        return pageObjects != null && !pageObjects.isEmpty();
    }

    public boolean hasClasses() {
        return classes != null && !classes.isEmpty();
    }
}
