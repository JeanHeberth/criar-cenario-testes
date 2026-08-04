package com.br.criarcenariotestes.business.autoqa.discovery.resolver;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AutomationFrameworkResolver - Testes Unitários")
class AutomationFrameworkResolverTest {

    private final AutomationFrameworkResolver resolver = new AutomationFrameworkResolver();

    @Test
    @DisplayName("Selenide deve prevalecer sobre Selenium")
    void selenideDevePrevalecerSobreSelenium() {
        AutomationFramework framework = resolver.resolve(
                Set.of(AutomationFramework.SELENIUM, AutomationFramework.SELENIDE),
                List.of()
        );

        assertThat(framework).isEqualTo(AutomationFramework.SELENIDE);
    }

    @Test
    @DisplayName("Playwright e Cypress devem resultar em UNKNOWN")
    void playwrightECypressDevemResultarEmUnknown() {
        AutomationFramework framework = resolver.resolve(
                Set.of(AutomationFramework.PLAYWRIGHT, AutomationFramework.CYPRESS),
                List.of()
        );

        assertThat(framework).isEqualTo(AutomationFramework.UNKNOWN);
    }

    @Test
    @DisplayName("Projeto híbrido web e API deve resultar em UNKNOWN")
    void projetoHibridoWebEApiDeveResultarEmUnknown() {
        AutomationFramework framework = resolver.resolve(
                Set.of(AutomationFramework.CYPRESS, AutomationFramework.REST_ASSURED),
                List.of()
        );

        assertThat(framework).isEqualTo(AutomationFramework.UNKNOWN);
    }
}
