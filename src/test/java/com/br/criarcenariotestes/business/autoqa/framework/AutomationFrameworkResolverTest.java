package com.br.criarcenariotestes.business.autoqa.framework;

import com.br.criarcenariotestes.business.autoqa.exception.UnsupportedFrameworkException;
import com.br.criarcenariotestes.business.autoqa.framework.cypress.CypressAdapter;
import com.br.criarcenariotestes.business.autoqa.framework.playwright.PlaywrightAdapter;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AutomationFrameworkResolver")
class AutomationFrameworkResolverTest {

    private AutomationFrameworkResolver resolver;

    @BeforeEach
    void setUp() {
        List<AutomationFrameworkAdapter> adapters = List.of(
                new PlaywrightAdapter(),
                new CypressAdapter()
        );
        resolver = new AutomationFrameworkResolver(adapters);
    }

    @Test
    @DisplayName("deve resolver PlaywrightAdapter para PLAYWRIGHT")
    void resolvesPlaywright() {
        AutomationFrameworkAdapter adapter = resolver.resolve(AutomationFramework.PLAYWRIGHT);
        assertThat(adapter).isInstanceOf(PlaywrightAdapter.class);
        assertThat(adapter.getFramework()).isEqualTo(AutomationFramework.PLAYWRIGHT);
    }

    @Test
    @DisplayName("deve resolver CypressAdapter para CYPRESS")
    void resolvesCypress() {
        AutomationFrameworkAdapter adapter = resolver.resolve(AutomationFramework.CYPRESS);
        assertThat(adapter).isInstanceOf(CypressAdapter.class);
        assertThat(adapter.getFramework()).isEqualTo(AutomationFramework.CYPRESS);
    }

    @Test
    @DisplayName("deve lançar UnsupportedFrameworkException para SELENIUM")
    void throwsForSelenium() {
        assertThatThrownBy(() -> resolver.resolve(AutomationFramework.SELENIUM))
                .isInstanceOf(UnsupportedFrameworkException.class);
    }

    @Test
    @DisplayName("deve lançar UnsupportedFrameworkException para UNKNOWN")
    void throwsForUnknown() {
        assertThatThrownBy(() -> resolver.resolve(AutomationFramework.UNKNOWN))
                .isInstanceOf(UnsupportedFrameworkException.class);
    }

    @Test
    @DisplayName("deve lançar UnsupportedFrameworkException para framework null")
    void throwsForNull() {
        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("compatibilidade: PlaywrightAdapter não suporta JAVA")
    void playwrightDoesNotSupportJava() {
        AutomationFrameworkAdapter adapter = resolver.resolve(AutomationFramework.PLAYWRIGHT);
        assertThat(adapter.supports(
                com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage.JAVA
        )).isFalse();
    }
}
