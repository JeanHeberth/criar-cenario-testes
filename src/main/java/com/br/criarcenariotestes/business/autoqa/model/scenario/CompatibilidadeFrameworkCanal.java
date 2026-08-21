package com.br.criarcenariotestes.business.autoqa.model.scenario;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Quais canais cada framework consegue automatizar.
 *
 * Framework e canal são eixos INDEPENDENTES, não redundantes: o Playwright faz
 * UI com {@code page}/locators e API com {@code APIRequestContext} — código
 * completamente diferente a partir da mesma dependência. Deduzir o canal do
 * framework, como se um implicasse o outro, escolheria errado metade das vezes.
 *
 * O que a matriz descarta é só a combinação impossível: REST Assured não abre
 * navegador, Selenium não faz requisição de API. Combinação possível passa,
 * ainda que incomum — quem escreve o teste sabe o que quer.
 *
 * É a fonte única da regra: o backend valida por ela e o frontend monta os
 * selects em cascata a partir dela (ver AutoQaCapabilitiesController), para não
 * existirem duas cópias divergentes.
 */
public final class CompatibilidadeFrameworkCanal {

    private static final Map<AutomationFramework, Set<AutomationType>> CANAIS_POR_FRAMEWORK =
            new EnumMap<>(AutomationFramework.class);

    static {
        CANAIS_POR_FRAMEWORK.put(AutomationFramework.PLAYWRIGHT,
                ordenado(AutomationType.WEB_UI, AutomationType.API));
        CANAIS_POR_FRAMEWORK.put(AutomationFramework.CYPRESS,
                ordenado(AutomationType.WEB_UI, AutomationType.API));
        CANAIS_POR_FRAMEWORK.put(AutomationFramework.ROBOT_FRAMEWORK,
                ordenado(AutomationType.WEB_UI, AutomationType.API, AutomationType.MOBILE));
        CANAIS_POR_FRAMEWORK.put(AutomationFramework.SELENIUM, ordenado(AutomationType.WEB_UI));
        CANAIS_POR_FRAMEWORK.put(AutomationFramework.SELENIDE, ordenado(AutomationType.WEB_UI));
        CANAIS_POR_FRAMEWORK.put(AutomationFramework.REST_ASSURED, ordenado(AutomationType.API));
        CANAIS_POR_FRAMEWORK.put(AutomationFramework.KARATE, ordenado(AutomationType.API));
        CANAIS_POR_FRAMEWORK.put(AutomationFramework.PACT, ordenado(AutomationType.INTEGRATION));
        CANAIS_POR_FRAMEWORK.put(AutomationFramework.APPIUM, ordenado(AutomationType.MOBILE));
    }

    private CompatibilidadeFrameworkCanal() {
    }

    /** Canais que o framework atende; vazio para framework desconhecido. */
    public static Set<AutomationType> canaisDe(AutomationFramework framework) {
        if (framework == null) {
            return Set.of();
        }
        return CANAIS_POR_FRAMEWORK.getOrDefault(framework, Set.of());
    }

    /**
     * Frameworks com canal conhecido, na ordem de declaração. UNKNOWN fica de
     * fora: não é uma escolha que o usuário possa fazer.
     */
    public static Set<AutomationFramework> frameworksSuportados() {
        return new LinkedHashSet<>(CANAIS_POR_FRAMEWORK.keySet());
    }

    /**
     * Aceita quando não há o que conferir — framework ou canal ausente/UNKNOWN
     * significa "deduza pelo projeto", e aí não existe conflito a apontar.
     */
    public static boolean compativel(AutomationFramework framework, AutomationType canal) {
        if (framework == null || framework == AutomationFramework.UNKNOWN) {
            return true;
        }
        if (canal == null || canal == AutomationType.UNKNOWN) {
            return true;
        }
        Set<AutomationType> canais = canaisDe(framework);
        return canais.isEmpty() || canais.contains(canal);
    }

    private static Set<AutomationType> ordenado(AutomationType... tipos) {
        return new LinkedHashSet<>(java.util.Arrays.asList(tipos));
    }
}
