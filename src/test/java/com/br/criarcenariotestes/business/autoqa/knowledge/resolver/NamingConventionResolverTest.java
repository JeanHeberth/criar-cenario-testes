package com.br.criarcenariotestes.business.autoqa.knowledge.resolver;

import com.br.criarcenariotestes.business.autoqa.knowledge.KnowledgeTestData;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.NamingConvention;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseConfidence;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NamingConventionResolver - Testes Unitários")
class NamingConventionResolverTest {

    private final NamingConventionResolver resolver = new NamingConventionResolver();

    @Test
    @DisplayName("Deve detectar padrão spec ts")
    void deveDetectarPadraoSpecTs() {
        NamingConvention convention = resolver.resolve(List.of(
                KnowledgeTestData.component("src/tests/login.spec.ts", "login.spec", ComponentType.TEST, SourceLanguage.TYPESCRIPT)
        ));

        assertThat(convention.testFilePattern()).isEqualTo("*.spec.ts");
    }

    @Test
    @DisplayName("Deve detectar padrão cy ts")
    void deveDetectarPadraoCyTs() {
        NamingConvention convention = resolver.resolve(List.of(
                KnowledgeTestData.component("src/tests/login.cy.ts", "login.cy", ComponentType.TEST, SourceLanguage.TYPESCRIPT)
        ));

        assertThat(convention.testFilePattern()).isEqualTo("*.cy.ts");
    }

    @Test
    @DisplayName("Deve detectar padrão java test")
    void deveDetectarPadraoJavaTest() {
        NamingConvention convention = resolver.resolve(List.of(
                KnowledgeTestData.component("src/test/java/LoginTest.java", "LoginTest", ComponentType.TEST, SourceLanguage.JAVA)
        ));

        assertThat(convention.testFilePattern()).isEqualTo("*Test.java");
    }

    @Test
    @DisplayName("Deve detectar padrão page")
    void deveDetectarPadraoPage() {
        NamingConvention convention = resolver.resolve(List.of(
                KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT)
        ));

        assertThat(convention.pageObjectPattern()).isEqualTo("*Page.ts");
    }

    @Test
    @DisplayName("Deve retornar unknown sem evidência")
    void deveRetornarUnknownSemEvidencia() {
        NamingConvention convention = resolver.resolve(List.of());

        assertThat(convention.confidence()).isEqualTo(ReuseConfidence.UNKNOWN);
        assertThat(convention.testFilePattern()).isNull();
        assertThat(convention.pageObjectPattern()).isNull();
    }

    @Test
    @DisplayName("Deve calcular confiança alta para padrão consistente")
    void deveCalcularConfiancaAltaParaPadraoConsistente() {
        NamingConvention convention = resolver.resolve(List.of(
                KnowledgeTestData.component("src/tests/a.spec.ts", "a.spec", ComponentType.TEST, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/tests/b.spec.ts", "b.spec", ComponentType.TEST, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/tests/c.spec.ts", "c.spec", ComponentType.TEST, SourceLanguage.TYPESCRIPT)
        ));

        assertThat(convention.confidence()).isEqualTo(ReuseConfidence.HIGH);
    }

    @Test
    @DisplayName("Deve calcular confiança baixa com poucos arquivos")
    void deveCalcularConfiancaBaixaComPoucosArquivos() {
        NamingConvention convention = resolver.resolve(List.of(
                KnowledgeTestData.component("src/tests/a.spec.ts", "a.spec", ComponentType.TEST, SourceLanguage.TYPESCRIPT)
        ));

        assertThat(convention.confidence()).isEqualTo(ReuseConfidence.LOW);
    }

    @Test
    @DisplayName("Deve limitar exemplos")
    void deveLimitarExemplos() {
        NamingConvention convention = resolver.resolve(List.of(
                KnowledgeTestData.component("src/tests/a.spec.ts", "a.spec", ComponentType.TEST, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/tests/b.spec.ts", "b.spec", ComponentType.TEST, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/tests/c.spec.ts", "c.spec", ComponentType.TEST, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/tests/d.spec.ts", "d.spec", ComponentType.TEST, SourceLanguage.TYPESCRIPT)
        ));

        assertThat(convention.examples()).hasSize(3);
    }
}
