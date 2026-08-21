package com.br.criarcenariotestes.business.autoqa.model.scenario;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CompatibilidadeFrameworkCanal - Testes Unitários")
class CompatibilidadeFrameworkCanalTest {

    @Test
    @DisplayName("Playwright atende UI e API — o framework não decide o canal sozinho")
    void playwrightAtendeUiEApi() {
        assertThat(CompatibilidadeFrameworkCanal.canaisDe(AutomationFramework.PLAYWRIGHT))
                .containsExactly(AutomationType.WEB_UI, AutomationType.API);
        assertThat(CompatibilidadeFrameworkCanal.compativel(AutomationFramework.PLAYWRIGHT, AutomationType.API)).isTrue();
        assertThat(CompatibilidadeFrameworkCanal.compativel(AutomationFramework.PLAYWRIGHT, AutomationType.WEB_UI)).isTrue();
    }

    @Test
    @DisplayName("Deve recusar combinação impossível")
    void deveRecusarCombinacaoImpossivel() {
        assertThat(CompatibilidadeFrameworkCanal.compativel(AutomationFramework.REST_ASSURED, AutomationType.WEB_UI)).isFalse();
        assertThat(CompatibilidadeFrameworkCanal.compativel(AutomationFramework.SELENIDE, AutomationType.API)).isFalse();
        assertThat(CompatibilidadeFrameworkCanal.compativel(AutomationFramework.APPIUM, AutomationType.WEB_UI)).isFalse();
    }

    @Test
    @DisplayName("Ausente ou UNKNOWN significa 'deduza pelo projeto' — não é conflito")
    void ausenteOuUnknownNaoEConflito() {
        assertThat(CompatibilidadeFrameworkCanal.compativel(null, AutomationType.WEB_UI)).isTrue();
        assertThat(CompatibilidadeFrameworkCanal.compativel(AutomationFramework.REST_ASSURED, null)).isTrue();
        assertThat(CompatibilidadeFrameworkCanal.compativel(AutomationFramework.UNKNOWN, AutomationType.WEB_UI)).isTrue();
        assertThat(CompatibilidadeFrameworkCanal.compativel(AutomationFramework.REST_ASSURED, AutomationType.UNKNOWN)).isTrue();
    }

    @Test
    @DisplayName("Todo framework suportado oferece ao menos um canal, e UNKNOWN não é ofertável")
    void frameworksSuportadosSaoConsistentes() {
        assertThat(CompatibilidadeFrameworkCanal.frameworksSuportados())
                .isNotEmpty()
                .doesNotContain(AutomationFramework.UNKNOWN)
                .allSatisfy(f -> assertThat(CompatibilidadeFrameworkCanal.canaisDe(f)).isNotEmpty());
    }
}
