package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.generation.GenerationTestData;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewCategory;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewIssue;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CodeReviewInputSanitizer - Testes Unitários")
class CodeReviewInputSanitizerTest {

    private CodeReviewInputSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new CodeReviewInputSanitizer();
    }

    @Test
    @DisplayName("Deve nunca incluir projectPath")
    void deveRemoverProjectPath() {
        SanitizedCodeReviewInput result = sanitize(artifact("tests/login.spec.ts", "conteudo"), List.of());
        assertThat(result.toString()).doesNotContain("/project");
    }

    @Test
    @DisplayName("Deve redigir segredo em formato chave=valor")
    void deveRedigirSegredo() {
        String content = "const password = \"SuperSecreta123\";";
        SanitizedCodeReviewInput result = sanitize(artifact("tests/login.spec.ts", content), List.of());
        assertThat(result.files().get(0).content()).contains("[REDACTED]");
        assertThat(result.files().get(0).content()).doesNotContain("SuperSecreta123");
    }

    @Test
    @DisplayName("Deve redigir token Bearer")
    void deveRedigirToken() {
        String content = "headers: { Authorization: 'Bearer abcdef123456' }";
        SanitizedCodeReviewInput result = sanitize(artifact("tests/login.spec.ts", content), List.of());
        assertThat(result.files().get(0).content()).contains("[REDACTED]");
        assertThat(result.files().get(0).content()).doesNotContain("abcdef123456");
    }

    @Test
    @DisplayName("Deve redigir credencial genérica")
    void deveRedigirCredencial() {
        String content = "senha: 'MinhaSenha!2024'";
        SanitizedCodeReviewInput result = sanitize(artifact("tests/login.spec.ts", content), List.of());
        assertThat(result.files().get(0).content()).doesNotContain("MinhaSenha!2024");
    }

    @Test
    @DisplayName("Deve redigir URL com credenciais preservando o restante da URL")
    void deveRedigirUrlComCredencial() {
        String content = "const url = 'https://user:segredo123@example.com/api';";
        SanitizedCodeReviewInput result = sanitize(artifact("tests/login.spec.ts", content), List.of());
        String sanitizedContent = result.files().get(0).content();
        assertThat(sanitizedContent).doesNotContain("segredo123");
        assertThat(sanitizedContent).contains("example.com/api");
    }

    @Test
    @DisplayName("Deve manter a estrutura do código (sem quebrar sintaxe)")
    void deveManterEstruturaDoCodigo() {
        String content = "const password = \"abc123\";\nconsole.log('ok');";
        SanitizedCodeReviewInput result = sanitize(artifact("tests/login.spec.ts", content), List.of());
        assertThat(result.files().get(0).content()).contains("const password =").contains("console.log('ok');");
    }

    @Test
    @DisplayName("Deve limitar arquivos a MAX_FILES")
    void deveLimitarArquivos() {
        List<GeneratedArtifactReader.ReadArtifact> artifacts = IntStream.rangeClosed(1, 25)
                .mapToObj(i -> artifact("tests/test" + String.format("%02d", i) + ".spec.ts", "conteudo " + i))
                .collect(Collectors.toList());

        SanitizedCodeReviewInput result = sanitizeMulti(artifacts, List.of());

        assertThat(result.files()).hasSize(CodeReviewInputSanitizer.MAX_FILES);
    }

    @Test
    @DisplayName("Deve limitar conteúdo por arquivo a MAX_CONTENT_LENGTH")
    void deveLimitarConteudoPorArquivo() {
        String longContent = "x".repeat(CodeReviewInputSanitizer.MAX_CONTENT_LENGTH + 500);
        SanitizedCodeReviewInput result = sanitize(artifact("tests/login.spec.ts", longContent), List.of());
        assertThat(result.files().get(0).content()).hasSize(CodeReviewInputSanitizer.MAX_CONTENT_LENGTH);
    }

    @Test
    @DisplayName("Deve limitar issues estáticas a MAX_STATIC_ISSUES")
    void deveLimitarIssues() {
        List<ReviewIssue> issues = IntStream.rangeClosed(1, 40)
                .mapToObj(i -> new ReviewIssue("CODE" + i, ReviewCategory.CODE_QUALITY, ReviewSeverity.LOW,
                        "tests/x.ts", null, "msg", null, "rec", false))
                .collect(Collectors.toList());

        SanitizedCodeReviewInput result = sanitize(artifact("tests/login.spec.ts", "conteudo"), issues);

        assertThat(result.staticIssues()).hasSize(CodeReviewInputSanitizer.MAX_STATIC_ISSUES);
    }

    @Test
    @DisplayName("Deve ser determinístico com as mesmas entradas")
    void deveSerDeterministico() {
        GeneratedArtifactReader.ReadArtifact a = artifact("tests/login.spec.ts", "conteudo");
        List<ReviewIssue> issues = List.of(new ReviewIssue("CODE", ReviewCategory.CODE_QUALITY, ReviewSeverity.LOW,
                "tests/login.spec.ts", null, "msg", null, "rec", false));

        SanitizedCodeReviewInput r1 = sanitize(a, issues);
        SanitizedCodeReviewInput r2 = sanitize(a, issues);

        assertThat(r1).isEqualTo(r2);
    }

    @Test
    @DisplayName("Deve não modificar objetos originais")
    void deveNaoModificarObjetosOriginais() {
        GeneratedArtifactReader.ReadArtifact a = artifact("tests/login.spec.ts", "const password = \"abc\";");
        String originalContent = a.content();

        sanitize(a, List.of());

        assertThat(a.content()).isEqualTo(originalContent);
    }

    @Test
    @DisplayName("Deve respeitar limite total razoável")
    void deveRespeitarLimiteTotal() {
        List<GeneratedArtifactReader.ReadArtifact> artifacts = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> artifact("tests/t" + i + ".spec.ts", "conteudo"))
                .collect(Collectors.toList());
        List<ReviewIssue> issues = IntStream.rangeClosed(1, 40)
                .mapToObj(i -> new ReviewIssue("CODE" + i, ReviewCategory.CODE_QUALITY, ReviewSeverity.LOW,
                        "tests/x.ts", null, "msg", null, "rec", false))
                .collect(Collectors.toList());

        SanitizedCodeReviewInput result = sanitizeMulti(artifacts, issues);

        assertThat(result.files().size()).isLessThanOrEqualTo(CodeReviewInputSanitizer.MAX_FILES);
        assertThat(result.staticIssues().size()).isLessThanOrEqualTo(CodeReviewInputSanitizer.MAX_STATIC_ISSUES);
    }

    @Test
    @DisplayName("Deve incluir framework, linguagem e plano no output")
    void deveIncluirFrameworkLinguagemEPlano() {
        SanitizedCodeReviewInput result = sanitize(artifact("tests/login.spec.ts", "conteudo"), List.of());
        assertThat(result.framework()).isNotNull();
        assertThat(result.language()).isNotNull();
        assertThat(result.planTitle()).isNotNull();
    }

    // --- helpers ---

    private GeneratedArtifactReader.ReadArtifact artifact(String path, String content) {
        return new GeneratedArtifactReader.ReadArtifact(path, GeneratedFileOperation.CREATE, PlanComponentType.TEST, content, "hash", true);
    }

    private SanitizedCodeReviewInput sanitize(GeneratedArtifactReader.ReadArtifact artifact, List<ReviewIssue> issues) {
        return sanitizeMulti(List.of(artifact), issues);
    }

    private SanitizedCodeReviewInput sanitizeMulti(List<GeneratedArtifactReader.ReadArtifact> artifacts, List<ReviewIssue> issues) {
        ProjectKnowledgeResult knowledge = GenerationTestData.completeKnowledge();
        return sanitizer.sanitize(
                GenerationTestData.playwrightDiscovery(), GenerationTestData.validScenario(), knowledge,
                GenerationTestData.readyPlan(GenerationTestData.createAction("tests/login.spec.ts", PlanComponentType.TEST)),
                artifacts, issues
        );
    }
}
