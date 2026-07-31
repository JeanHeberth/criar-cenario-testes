package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedCodeResponse;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFileMetadata;
import com.br.criarcenariotestes.business.autoqa.model.enums.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GeneratedFileStorageService")
class GeneratedFileStorageServiceTest {

    @TempDir
    Path projectDir;

    private GeneratedFileStorageService service;
    private static final String EXEC_ID = "test-execution-123";

    @BeforeEach
    void setUp() {
        AutoQaProperties props = new AutoQaProperties();
        props.setGeneratedDirectory(".auto-qa/generated");
        service = new GeneratedFileStorageService(props);
    }

    private GeneratedCodeResponse singleFileResponse(String relativePath) {
        return new GeneratedCodeResponse(
                List.of(new GeneratedFile(
                        relativePath, GeneratedFileOperation.CREATE,
                        "test('login', async () => {});", "Arquivo de teste", null
                )),
                List.of("LoginPage"), List.of(), List.of(), "Test generated", false, null
        );
    }

    // ─── Diretório gerado ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Estrutura de diretórios")
    class DirectoryStructure {

        @Test
        @DisplayName("deve criar diretório .auto-qa/generated/<executionId>/files/")
        void createsGeneratedDirectory() {
            service.store(EXEC_ID, projectDir, singleFileResponse("tests/login.spec.ts"));
            Path expectedDir = projectDir.resolve(".auto-qa").resolve("generated")
                    .resolve(EXEC_ID).resolve("files");
            assertThat(expectedDir).exists();
        }

        @Test
        @DisplayName("deve criar subdiretório para arquivo aninhado")
        void createsNestedSubdirectory() {
            service.store(EXEC_ID, projectDir, singleFileResponse("tests/login/login.spec.ts"));
            Path file = projectDir.resolve(".auto-qa").resolve("generated")
                    .resolve(EXEC_ID).resolve("files")
                    .resolve("tests").resolve("login").resolve("login.spec.ts");
            assertThat(file).exists();
        }

        @Test
        @DisplayName("resolveGeneratedDir deve retornar path correto")
        void resolvesGeneratedDir() {
            Path dir = service.resolveGeneratedDir(EXEC_ID, projectDir);
            assertThat(dir.toString())
                    .contains(".auto-qa")
                    .contains("generated")
                    .contains(EXEC_ID);
        }
    }

    // ─── Conteúdo do arquivo ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Conteúdo dos arquivos")
    class FileContent {

        @Test
        @DisplayName("deve escrever conteúdo no arquivo gerado")
        void writesContentToFile() throws IOException {
            service.store(EXEC_ID, projectDir, singleFileResponse("tests/login.spec.ts"));
            Path file = projectDir.resolve(".auto-qa").resolve("generated")
                    .resolve(EXEC_ID).resolve("files").resolve("tests").resolve("login.spec.ts");
            assertThat(Files.readString(file)).contains("test('login'");
        }

        @Test
        @DisplayName("deve criar manifest.json no diretório de execução")
        void createsManifest() {
            service.store(EXEC_ID, projectDir, singleFileResponse("tests/login.spec.ts"));
            Path manifest = projectDir.resolve(".auto-qa").resolve("generated")
                    .resolve(EXEC_ID).resolve("manifest.json");
            assertThat(manifest).exists();
        }

        @Test
        @DisplayName("manifest.json deve conter executionId")
        void manifestContainsExecutionId() throws IOException {
            service.store(EXEC_ID, projectDir, singleFileResponse("tests/login.spec.ts"));
            Path manifest = projectDir.resolve(".auto-qa").resolve("generated")
                    .resolve(EXEC_ID).resolve("manifest.json");
            assertThat(Files.readString(manifest)).contains(EXEC_ID);
        }
    }

    // ─── Metadados retornados ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Metadados retornados")
    class MetadataResult {

        @Test
        @DisplayName("deve retornar lista não nula de metadados")
        void returnsNonNullMetadata() {
            List<GeneratedFileMetadata> result = service.store(
                    EXEC_ID, projectDir, singleFileResponse("tests/login.spec.ts")
            );
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("deve retornar metadado com relativePath correto")
        void metadataHasRelativePath() {
            List<GeneratedFileMetadata> result = service.store(
                    EXEC_ID, projectDir, singleFileResponse("tests/login.spec.ts")
            );
            assertThat(result).anyMatch(m -> "tests/login.spec.ts".equals(m.relativePath()));
        }

        @Test
        @DisplayName("deve gerar hash não nulo no metadado")
        void metadataHasHash() {
            List<GeneratedFileMetadata> result = service.store(
                    EXEC_ID, projectDir, singleFileResponse("tests/login.spec.ts")
            );
            assertThat(result).allMatch(m -> m.generatedHash() != null && !m.generatedHash().isBlank());
        }

        @Test
        @DisplayName("deve retornar lista vazia para response sem arquivos")
        void emptyForEmptyResponse() {
            GeneratedCodeResponse empty = new GeneratedCodeResponse(
                    List.of(), List.of(), List.of(), List.of(), "none", false, null
            );
            List<GeneratedFileMetadata> result = service.store(EXEC_ID, projectDir, empty);
            assertThat(result).isEmpty();
        }
    }

    // ─── Validações de segurança ──────────────────────────────────────────────

    @Nested
    @DisplayName("Validações de segurança")
    class SecurityValidations {

        @Test
        @DisplayName("deve ignorar arquivo com path absoluto Unix")
        void ignoresAbsoluteUnixPath() {
            GeneratedCodeResponse response = singleFileResponse("/etc/passwd");
            List<GeneratedFileMetadata> result = service.store(EXEC_ID, projectDir, response);
            assertThat(result).isEmpty();
            // Não deve criar o arquivo
            assertThat(projectDir.resolve("etc").resolve("passwd")).doesNotExist();
        }

        @Test
        @DisplayName("deve ignorar arquivo com path traversal")
        void ignoresPathTraversal() {
            GeneratedCodeResponse response = singleFileResponse("../../secret.ts");
            List<GeneratedFileMetadata> result = service.store(EXEC_ID, projectDir, response);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("arquivos gerados ficam dentro do diretório .auto-qa — nunca na raiz do projeto")
        void filesStayInGeneratedDir() throws IOException {
            service.store(EXEC_ID, projectDir, singleFileResponse("tests/login.spec.ts"));
            // O arquivo tests/login.spec.ts não deve existir diretamente no projeto
            assertThat(projectDir.resolve("tests").resolve("login.spec.ts")).doesNotExist();
            // Deve existir dentro de .auto-qa
            Path stored = projectDir.resolve(".auto-qa").resolve("generated")
                    .resolve(EXEC_ID).resolve("files").resolve("tests").resolve("login.spec.ts");
            assertThat(stored).exists();
        }
    }
}
