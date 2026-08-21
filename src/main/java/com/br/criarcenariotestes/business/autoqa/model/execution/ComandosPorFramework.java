package com.br.criarcenariotestes.business.autoqa.model.execution;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Quais comandos de teste fazem sentido para cada framework.
 *
 * Espelha o que o CommandResolver realmente propõe: ele só monta candidatos do
 * framework detectado, então oferecer os onze comandos no formulário de
 * aprovação obrigava o usuário a adivinhar qual marcar — e marcar errado fazia
 * o resolvedor não achar comando nenhum e devolver BLOCKED, já com o pipeline
 * inteiro gasto.
 *
 * Fica no backend, como a matriz framework→canal, para o frontend não manter
 * uma cópia que diverge no primeiro comando novo.
 */
public final class ComandosPorFramework {

    private static final Map<AutomationFramework, Set<String>> COMANDOS =
            new EnumMap<>(AutomationFramework.class);

    static {
        COMANDOS.put(AutomationFramework.PLAYWRIGHT, ordenado("PLAYWRIGHT_TEST", "NPM_TEST", "NPM_TEST_E2E"));
        COMANDOS.put(AutomationFramework.CYPRESS, ordenado("CYPRESS_RUN", "CYPRESS_SCRIPT_RUN", "NPM_TEST", "NPM_TEST_E2E"));
        COMANDOS.put(AutomationFramework.SELENIDE, ordenado("GRADLE_WRAPPER_TEST", "GRADLE_WRAPPER_CLEAN_TEST", "MAVEN_WRAPPER_TEST", "MAVEN_TEST"));
        COMANDOS.put(AutomationFramework.SELENIUM, ordenado("GRADLE_WRAPPER_TEST", "GRADLE_WRAPPER_CLEAN_TEST", "MAVEN_WRAPPER_TEST", "MAVEN_TEST"));
        COMANDOS.put(AutomationFramework.REST_ASSURED, ordenado("GRADLE_WRAPPER_TEST", "GRADLE_WRAPPER_CLEAN_TEST", "MAVEN_WRAPPER_TEST", "MAVEN_TEST"));
        COMANDOS.put(AutomationFramework.KARATE, ordenado("GRADLE_WRAPPER_TEST", "GRADLE_WRAPPER_CLEAN_TEST", "MAVEN_WRAPPER_TEST", "MAVEN_TEST"));
        COMANDOS.put(AutomationFramework.PACT, ordenado("GRADLE_WRAPPER_TEST", "MAVEN_TEST"));
        COMANDOS.put(AutomationFramework.ROBOT_FRAMEWORK, ordenado("ROBOT_TEST"));
        COMANDOS.put(AutomationFramework.APPIUM, ordenado("GRADLE_WRAPPER_TEST", "MAVEN_TEST", "PYTEST"));
    }

    private ComandosPorFramework() {
    }

    /** Vazio para framework desconhecido — aí o formulário oferece todos. */
    public static Set<String> de(AutomationFramework framework) {
        if (framework == null) {
            return Set.of();
        }
        return COMANDOS.getOrDefault(framework, Set.of());
    }

    private static Set<String> ordenado(String... comandos) {
        return new LinkedHashSet<>(java.util.Arrays.asList(comandos));
    }
}
