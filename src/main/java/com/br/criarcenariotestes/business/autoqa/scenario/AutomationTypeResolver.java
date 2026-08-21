package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class AutomationTypeResolver {

    private static final Logger log = LoggerFactory.getLogger(AutomationTypeResolver.class);

    private static final Map<AutomationFramework, AutomationType> POR_FRAMEWORK = Map.of(
            AutomationFramework.REST_ASSURED, AutomationType.API,
            AutomationFramework.KARATE, AutomationType.API,
            AutomationFramework.PACT, AutomationType.INTEGRATION,
            AutomationFramework.PLAYWRIGHT, AutomationType.WEB_UI,
            AutomationFramework.CYPRESS, AutomationType.WEB_UI,
            AutomationFramework.SELENIUM, AutomationType.WEB_UI,
            AutomationFramework.SELENIDE, AutomationType.WEB_UI,
            AutomationFramework.APPIUM, AutomationType.MOBILE
    );

    /**
     * Chaves SEM hífen nem underscore de propósito: o discovery devolve as
     * bibliotecas em formatos diferentes conforme a origem — "REST_ASSURED"
     * (nome do detector) e "io.rest-assured:rest-assured" (coordenada Gradle).
     * Normalizar os dois lados removendo separadores faz as duas formas caírem
     * na mesma chave; comparar com hífen só casava uma delas.
     */
    private static final Map<String, AutomationType> POR_BIBLIOTECA = Map.ofEntries(
            Map.entry("restassured", AutomationType.API),
            Map.entry("karate", AutomationType.API),
            Map.entry("retrofit", AutomationType.API),
            Map.entry("okhttp", AutomationType.API),
            Map.entry("selenium", AutomationType.WEB_UI),
            Map.entry("selenide", AutomationType.WEB_UI),
            Map.entry("playwright", AutomationType.WEB_UI),
            Map.entry("cypress", AutomationType.WEB_UI),
            Map.entry("appium", AutomationType.MOBILE),
            Map.entry("pact", AutomationType.INTEGRATION)
    );

    private static String normalizar(String valor) {
        return valor.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }

    public AutomationType resolver(AutomationType daAnalise, ProjectDiscoveryResult discovery) {
        return resolver(daAnalise, discovery, null);
    }

    /**
     * @param informado canal escolhido pelo usuário no formulário, ou null.
     *
     * Vem PRIMEIRO, à frente até do que a IA respondeu: é uma decisão explícita
     * de quem está criando o teste, e num projeto que usa REST Assured e
     * Selenium ao mesmo tempo nenhuma heurística acerta a intenção.
     */
    public AutomationType resolver(AutomationType daAnalise, ProjectDiscoveryResult discovery,
                                    AutomationType informado) {
        if (informado != null && informado != AutomationType.UNKNOWN) {
            return informado;
        }
        if (daAnalise != null && daAnalise != AutomationType.UNKNOWN) {
            return daAnalise;
        }
        if (discovery == null) {
            return daAnalise;
        }

        AutomationType porFrameworkPrimario = POR_FRAMEWORK.get(discovery.getAutomationFramework());
        if (porFrameworkPrimario != null) {
            log.debug("AutomationType resolvido via framework primário: {} -> {}", discovery.getAutomationFramework(), porFrameworkPrimario);
            return porFrameworkPrimario;
        }

        AutomationType porDetectados = resolverPorDetectedFrameworks(discovery.getDetectedFrameworks());
        if (porDetectados != null) {
            log.debug("AutomationType resolvido via detectedFrameworks: {}", porDetectados);
            return porDetectados;
        }

        AutomationType porBibliotecas = resolverPorBibliotecas(discovery.getLibraries());
        if (porBibliotecas != null) {
            log.debug("AutomationType resolvido via libraries: {}", porBibliotecas);
            return porBibliotecas;
        }

        log.warn("AutomationType não pôde ser determinado. framework={}, detectedFrameworks={}, librariesCount={}",
                discovery.getAutomationFramework(), discovery.getDetectedFrameworks(),
                discovery.getLibraries() == null ? 0 : discovery.getLibraries().size());
        return daAnalise;
    }

    private AutomationType resolverPorDetectedFrameworks(Set<AutomationFramework> detectados) {
        if (detectados == null || detectados.isEmpty()) {
            return null;
        }
        AutomationType resultado = null;
        for (AutomationFramework fw : detectados) {
            AutomationType tipo = POR_FRAMEWORK.get(fw);
            if (tipo == null) continue;
            if (resultado == null) {
                resultado = tipo;
            } else if (resultado != tipo) {
                return AutomationType.HYBRID;
            }
        }
        return resultado;
    }

    private AutomationType resolverPorBibliotecas(List<String> libraries) {
        if (libraries == null || libraries.isEmpty()) {
            return null;
        }
        AutomationType resultado = null;
        for (String lib : libraries) {
            if (lib == null) continue;
            String normalizado = normalizar(lib);
            for (Map.Entry<String, AutomationType> entry : POR_BIBLIOTECA.entrySet()) {
                if (normalizado.contains(entry.getKey())) {
                    AutomationType tipo = entry.getValue();
                    if (resultado == null) {
                        resultado = tipo;
                    } else if (resultado != tipo) {
                        return AutomationType.HYBRID;
                    }
                    break;
                }
            }
        }
        return resultado;
    }
}
