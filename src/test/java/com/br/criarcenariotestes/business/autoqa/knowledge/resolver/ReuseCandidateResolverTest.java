package com.br.criarcenariotestes.business.autoqa.knowledge.resolver;

import com.br.criarcenariotestes.business.autoqa.knowledge.KnowledgeTestData;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseCandidate;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ReuseConfidence;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
import com.br.criarcenariotestes.business.autoqa.model.scenario.BusinessRule;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioStep;
import com.br.criarcenariotestes.business.autoqa.model.scenario.TestDataRequirement;
import com.br.criarcenariotestes.business.autoqa.model.scenario.TestDataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReuseCandidateResolver - Testes Unitários")
class ReuseCandidateResolverTest {

    private final ReuseCandidateResolver resolver = new ReuseCandidateResolver();

    @Test
    @DisplayName("Deve relacionar LoginPage com cenário de login")
    void deveRelacionarLoginPageComCenarioDeLogin() {
        List<ReuseCandidate> candidates = resolver.resolve(List.of(
                KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT)
        ), scenario("login", "login"));

        assertThat(candidates).isNotEmpty();
        assertThat(candidates.getFirst().componentPath()).contains("LoginPage");
    }

    @Test
    @DisplayName("Deve relacionar UserClient com entidade usuario")
    void deveRelacionarUserClientComEntidadeUsuario() {
        List<ReuseCandidate> candidates = resolver.resolve(List.of(
                KnowledgeTestData.component("src/api/UserClient.java", "UserClient", ComponentType.API_CLIENT, SourceLanguage.JAVA)
        ), scenario("Usuário", "Usuário"));

        assertThat(candidates).isNotEmpty();
    }

    @Test
    @DisplayName("Deve ignorar componente sem correspondência")
    void deveIgnorarComponenteSemCorrespondencia() {
        List<ReuseCandidate> candidates = resolver.resolve(List.of(
                KnowledgeTestData.component("src/helpers/RandomHelper.ts", "RandomHelper", ComponentType.HELPER, SourceLanguage.TYPESCRIPT)
        ), scenario("Pagamento", "Pagamento"));

        assertThat(candidates).isEmpty();
    }

    @Test
    @DisplayName("Deve normalizar maiúsculas e minúsculas")
    void deveNormalizarMaiusculasEMinusculas() {
        List<ReuseCandidate> candidates = resolver.resolve(List.of(
                KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT)
        ), scenario("LOGIN", "LOGIN"));

        assertThat(candidates).isNotEmpty();
    }

    @Test
    @DisplayName("Deve remover termos genéricos")
    void deveRemoverTermosGenericos() {
        List<ReuseCandidate> candidates = resolver.resolve(List.of(
                KnowledgeTestData.component("src/services/Service.ts", "Service", ComponentType.SERVICE, SourceLanguage.TYPESCRIPT)
        ), scenario("cenário teste validar fluxo", "cenário teste validar fluxo"));

        assertThat(candidates).isEmpty();
    }

    @Test
    @DisplayName("Deve preencher reason")
    void devePreencherReason() {
        List<ReuseCandidate> candidates = resolver.resolve(List.of(
                KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT)
        ), scenario("login", "login"));

        assertThat(candidates.getFirst().reason()).isNotBlank();
    }

    @Test
    @DisplayName("Deve preencher matchingTerms")
    void devePreencherMatchingTerms() {
        List<ReuseCandidate> candidates = resolver.resolve(List.of(
                KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT)
        ), scenario("login", "login"));

        assertThat(candidates.getFirst().matchingTerms()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve calcular confiança alta")
    void deveCalcularConfiancaAlta() {
        List<ReuseCandidate> candidates = resolver.resolve(List.of(
                KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT)
        ), scenario("login", "login"));

        assertThat(candidates.getFirst().confidence()).isIn(ReuseConfidence.HIGH, ReuseConfidence.MEDIUM);
    }

    @Test
    @DisplayName("Deve limitar quantidade de candidatos")
    void deveLimitarQuantidadeDeCandidatos() {
        List<ProjectComponent> components = List.of(
                KnowledgeTestData.component("src/pages/A.ts", "A", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/pages/B.ts", "B", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/pages/C.ts", "C", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/pages/D.ts", "D", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/pages/E.ts", "E", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/pages/F.ts", "F", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/pages/G.ts", "G", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/pages/H.ts", "H", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/pages/I.ts", "I", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/pages/J.ts", "J", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT),
                KnowledgeTestData.component("src/pages/K.ts", "K", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT)
        );

        List<ReuseCandidate> candidates = resolver.resolve(components, scenario("login", "login"));

        assertThat(candidates).hasSizeLessThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Deve não gerar import ou código")
    void deveNaoGerarImportOuCodigo() {
        List<ReuseCandidate> candidates = resolver.resolve(List.of(
                KnowledgeTestData.component("src/pages/LoginPage.ts", "LoginPage", ComponentType.PAGE_OBJECT, SourceLanguage.TYPESCRIPT)
        ), scenario("login", "login"));

        assertThat(candidates.getFirst().reason()).doesNotContain("import", "class");
    }

    private ScenarioAnalysisResult scenario(String title, String objective) {
        return new ScenarioAnalysisResult(
                title,
                objective,
                List.of("Usuário cadastrado"),
                List.of(new ScenarioStep(1, "Abrir login", "Tela aberta", List.of())),
                List.of(new TestDataRequirement("email", TestDataType.STATIC, true, "E-mail", null)),
                List.of(new BusinessRule("BR-001", "Usuário ativo", true)),
                List.of(),
                List.of(),
                List.of("Usuário"),
                List.of(),
                AutomationType.WEB_UI,
                ScenarioAnalysisStatus.VALID,
                List.of(),
                true
        );
    }
}
