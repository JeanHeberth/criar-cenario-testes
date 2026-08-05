package com.br.criarcenariotestes.business.autoqa.knowledge.parser;

import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JavaMetadataParser - Testes Unitários")
class JavaMetadataParserTest {

    private final JavaMetadataParser parser = new JavaMetadataParser();

    @Test
    @DisplayName("Deve extrair package")
    void deveExtrairPackage() {
        var metadata = parser.parse(file("src/test/java/LoginPage.java", """
                package com.example.pages;
                public class LoginPage {}
                """));

        assertThat(metadata.packageName()).isEqualTo("com.example.pages");
    }

    @Test
    @DisplayName("Deve extrair imports")
    void deveExtrairImports() {
        var metadata = parser.parse(file("src/test/java/LoginPage.java", """
                import java.util.List;
                import org.junit.jupiter.api.Test;
                public class LoginPage {}
                """));

        assertThat(metadata.imports()).contains("java.util.List", "org.junit.jupiter.api.Test");
    }

    @Test
    @DisplayName("Deve extrair classe")
    void deveExtrairClasse() {
        var metadata = parser.parse(file("src/test/java/LoginPage.java", """
                public class LoginPage {}
                """));

        assertThat(metadata.declaredClasses()).contains("LoginPage");
        assertThat(metadata.language()).isEqualTo(SourceLanguage.JAVA);
    }

    @Test
    @DisplayName("Deve extrair extends e implements")
    void deveExtrairExtendsEImplements() {
        var metadata = parser.parse(file("src/test/java/LoginPage.java", """
                public class LoginPage extends BasePage implements AutoCloseable {}
                """));

        assertThat(metadata.hierarchy()).contains("BasePage", "AutoCloseable");
    }

    @Test
    @DisplayName("Deve extrair métodos públicos")
    void deveExtrairMetodosPublicos() {
        var metadata = parser.parse(file("src/test/java/LoginPage.java", """
                public class LoginPage {
                    public void open() {}
                    protected void close() {}
                }
                """));

        assertThat(metadata.declaredMethods()).contains("open", "close");
    }

    @Test
    @DisplayName("Deve extrair annotations")
    void deveExtrairAnnotations() {
        var metadata = parser.parse(file("src/test/java/LoginPageTest.java", """
                import org.junit.jupiter.api.Test;
                @Deprecated
                public class LoginPageTest {
                    @Test
                    public void shouldOpen() {}
                }
                """));

        assertThat(metadata.annotations()).contains("Deprecated", "Test");
        assertThat(metadata.tags()).contains("TEST");
    }

    @Test
    @DisplayName("Deve detectar Test")
    void deveDetectarTest() {
        var metadata = parser.parse(file("src/test/java/LoginPageTest.java", """
                import org.junit.jupiter.api.Test;
                public class LoginPageTest {
                    @Test
                    public void shouldOpen() {}
                }
                """));

        assertThat(metadata.testComponent()).isTrue();
    }

    @Test
    @DisplayName("Deve não armazenar corpo completo")
    void deveNaoArmazenarCorpoCompleto() {
        var metadata = parser.parse(file("src/test/java/LoginPage.java", """
                public class LoginPage {
                    public void open() {
                        String password = "secret";
                    }
                }
                """));

        assertThat(metadata.declaredMethods()).doesNotContain("String password = \"secret\";");
    }

    private KnowledgeScanResult.KnowledgeFile file(String path, String content) {
        return new KnowledgeScanResult.KnowledgeFile(path, path.substring(path.lastIndexOf('/') + 1), ".java", content.length(), content);
    }
}
