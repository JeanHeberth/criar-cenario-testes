package com.br.criarcenariotestes.business.autoqa.model.generation;

import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GeneratedFile - Testes Unitários")
class GeneratedFileTest {

    @Test
    @DisplayName("Deve criar GeneratedFile válido para CREATE")
    void deveCriarGeneratedFileValido() {
        GeneratedFile file = new GeneratedFile(
                "tests/login.spec.ts",
                GeneratedFileOperation.CREATE,
                PlanComponentType.TEST,
                "import { test } from '@playwright/test';",
                "UTF-8",
                "abc123",
                GeneratedFileStatus.GENERATED,
                false,
                List.of("pages/LoginPage.ts"),
                List.of(),
                List.of()
        );

        assertThat(file.relativePath()).isEqualTo("tests/login.spec.ts");
        assertThat(file.operation()).isEqualTo(GeneratedFileOperation.CREATE);
        assertThat(file.content()).isNotBlank();
    }

    @Test
    @DisplayName("Deve permitir content nulo em REUSE")
    void devePermitirConteudoNuloEmReuse() {
        GeneratedFile file = new GeneratedFile(
                "pages/LoginPage.ts",
                GeneratedFileOperation.REUSE,
                PlanComponentType.PAGE_OBJECT,
                null,
                "UTF-8",
                null,
                GeneratedFileStatus.SKIPPED,
                true,
                List.of(),
                List.of(),
                List.of()
        );

        assertThat(file.content()).isNull();
        assertThat(file.sha256()).isNull();
    }

    @Test
    @DisplayName("Deve normalizar encoding padrão UTF-8 quando nulo")
    void deveNormalizarEncodingPadraoUtf8QuandoNulo() {
        GeneratedFile file = new GeneratedFile(
                "tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "codigo", null, "hash", GeneratedFileStatus.GENERATED, false, List.of(), List.of(), List.of()
        );

        assertThat(file.encoding()).isEqualTo("UTF-8");
    }

    @Test
    @DisplayName("Enum GeneratedFileOperation não deve conter DELETE")
    void naoDeveConterOperacaoDelete() {
        assertThatThrownBy(() -> GeneratedFileOperation.valueOf("DELETE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve retornar coleções imutáveis")
    void deveRetornarColecoesImutaveis() {
        GeneratedFile file = new GeneratedFile(
                "tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "codigo", "UTF-8", "hash", GeneratedFileStatus.GENERATED, false, List.of(), List.of(), List.of()
        );

        assertThatThrownBy(() -> file.reusedComponents().add("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> file.dependencies().add("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> file.warnings().add(new GenerationWarning("C", "d", false))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve normalizar coleções nulas para vazias")
    void deveNormalizarColecoesNulasParaVazias() {
        GeneratedFile file = new GeneratedFile(
                "tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "codigo", "UTF-8", "hash", GeneratedFileStatus.GENERATED, false, null, null, null
        );

        assertThat(file.reusedComponents()).isEmpty();
        assertThat(file.dependencies()).isEmpty();
        assertThat(file.warnings()).isEmpty();
    }

    @Test
    @DisplayName("Deve remover espaços do relativePath e do sha256")
    void deveTrimarRelativePathESha256() {
        GeneratedFile file = new GeneratedFile(
                "  tests/login.spec.ts  ", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                "codigo", "UTF-8", "  abc123  ", GeneratedFileStatus.GENERATED, false, List.of(), List.of(), List.of()
        );

        assertThat(file.relativePath()).isEqualTo("tests/login.spec.ts");
        assertThat(file.sha256()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("Deve manter contrato JSON via ObjectMapper")
    void deveManterContratoJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
                {
                  "relativePath": "tests/login.spec.ts",
                  "operation": "CREATE",
                  "componentType": "TEST",
                  "content": "codigo",
                  "encoding": "UTF-8",
                  "existingFile": false,
                  "reusedComponents": ["pages/LoginPage.ts"],
                  "dependencies": [],
                  "warnings": []
                }
                """;

        GeneratedFile file = mapper.readValue(json, GeneratedFile.class);

        assertThat(file.relativePath()).isEqualTo("tests/login.spec.ts");
        assertThat(file.operation()).isEqualTo(GeneratedFileOperation.CREATE);
        assertThat(file.reusedComponents()).containsExactly("pages/LoginPage.ts");
    }

    @Test
    @DisplayName("Deve preservar código com quebras de linha no content")
    void devePreservarCodigoComQuebrasDeLinha() {
        String content = "import { test } from '@playwright/test';\n\ntest('login', async () => {});\n";
        GeneratedFile file = new GeneratedFile(
                "tests/login.spec.ts", GeneratedFileOperation.CREATE, PlanComponentType.TEST,
                content, "UTF-8", "hash", GeneratedFileStatus.GENERATED, false, List.of(), List.of(), List.of()
        );

        assertThat(file.content()).isEqualTo(content);
    }

    @Test
    @DisplayName("Deve aceitar existingFile coerente com REUSE (true)")
    void deveAceitarExistingFileCoerenteComReuse() {
        GeneratedFile file = new GeneratedFile(
                "pages/LoginPage.ts", GeneratedFileOperation.REUSE, PlanComponentType.PAGE_OBJECT,
                null, "UTF-8", null, GeneratedFileStatus.SKIPPED, true, List.of(), List.of(), List.of()
        );

        assertThat(file.existingFile()).isTrue();
    }
}
