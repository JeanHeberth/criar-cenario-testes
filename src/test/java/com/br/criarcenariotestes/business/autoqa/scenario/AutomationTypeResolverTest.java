package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.DiscoveryConfidence;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AutomationTypeResolver - Testes Unitários")
class AutomationTypeResolverTest {

    private final AutomationTypeResolver resolver = new AutomationTypeResolver();

    @Test
    @DisplayName("Canal informado pelo usuário vence a análise e o discovery")
    void informadoVenceTudo() {
        ProjectDiscoveryResult discovery = discovery(AutomationFramework.PLAYWRIGHT, Set.of(AutomationFramework.PLAYWRIGHT), List.of("PLAYWRIGHT"));

        assertThat(resolver.resolver(AutomationType.WEB_UI, discovery, AutomationType.API))
                .isEqualTo(AutomationType.API);
    }

    @Test
    @DisplayName("Canal informado UNKNOWN é ignorado — cai para a análise")
    void informadoUnknownEIgnorado() {
        ProjectDiscoveryResult discovery = discovery(AutomationFramework.UNKNOWN, Set.of(), List.of());

        assertThat(resolver.resolver(AutomationType.WEB_UI, discovery, AutomationType.UNKNOWN))
                .isEqualTo(AutomationType.WEB_UI);
    }

    @Test
    @DisplayName("Sem informado nem análise, deduz pelo framework primário")
    void deduzPeloFrameworkPrimario() {
        ProjectDiscoveryResult discovery = discovery(AutomationFramework.REST_ASSURED, Set.of(), List.of());

        assertThat(resolver.resolver(AutomationType.UNKNOWN, discovery, null))
                .isEqualTo(AutomationType.API);
    }

    @Test
    @DisplayName("Framework primário UNKNOWN: deduz pelos detectedFrameworks")
    void deduzPelosDetectedFrameworks() {
        ProjectDiscoveryResult discovery = discovery(AutomationFramework.UNKNOWN,
                Set.of(AutomationFramework.REST_ASSURED), List.of());

        assertThat(resolver.resolver(AutomationType.UNKNOWN, discovery, null))
                .isEqualTo(AutomationType.API);
    }

    @Test
    @DisplayName("Deduz por libraries mesmo com separador diferente (REST_ASSURED vs rest-assured)")
    void deduzPorLibrariesNormalizandoSeparador() {
        ProjectDiscoveryResult semFramework = discovery(AutomationFramework.UNKNOWN, Set.of(), List.of("REST_ASSURED"));
        assertThat(resolver.resolver(AutomationType.UNKNOWN, semFramework, null)).isEqualTo(AutomationType.API);

        ProjectDiscoveryResult coordenadaGradle = discovery(AutomationFramework.UNKNOWN, Set.of(),
                List.of("io.rest-assured:rest-assured:5.5.0"));
        assertThat(resolver.resolver(AutomationType.UNKNOWN, coordenadaGradle, null)).isEqualTo(AutomationType.API);
    }

    @Test
    @DisplayName("Frameworks de tipos diferentes resultam em HYBRID")
    void frameworksDeTiposDiferentesViramHybrid() {
        ProjectDiscoveryResult discovery = discovery(AutomationFramework.UNKNOWN,
                Set.of(AutomationFramework.REST_ASSURED, AutomationFramework.SELENIUM), List.of());

        assertThat(resolver.resolver(AutomationType.UNKNOWN, discovery, null))
                .isEqualTo(AutomationType.HYBRID);
    }

    @Test
    @DisplayName("Nada dedutível devolve o valor da análise (UNKNOWN)")
    void nadaDedutivelDevolveAnalise() {
        ProjectDiscoveryResult discovery = discovery(AutomationFramework.UNKNOWN, Set.of(), List.of());

        assertThat(resolver.resolver(AutomationType.UNKNOWN, discovery, null))
                .isEqualTo(AutomationType.UNKNOWN);
    }

    private ProjectDiscoveryResult discovery(AutomationFramework framework,
                                             Set<AutomationFramework> detectados,
                                             List<String> libraries) {
        return new ProjectDiscoveryResult(
                Path.of("/projeto"), framework, AutomationLanguage.JAVA, PackageManager.UNKNOWN,
                BuildTool.GRADLE, Set.of(), detectados, libraries, "build.gradle",
                List.of("build.gradle"), List.of(), DiscoveryConfidence.MEDIUM, true);
    }
}
