package com.br.criarcenariotestes.business.autoqa.model.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GenerationManifest - Testes Unitários")
class GenerationManifestTest {

    @Test
    @DisplayName("Deve criar manifest válido com arquivo aninhado")
    void deveCriarManifestValido() {
        UUID executionId = UUID.randomUUID();
        GenerationManifest manifest = new GenerationManifest(
                executionId, "PLAYWRIGHT", "TYPESCRIPT", "READY", "COMPLETED", "2026-08-05T10:00:00",
                List.of(new GenerationManifest.GenerationManifestFile(
                        "tests/login.spec.ts", "CREATE", "TEST", "GENERATED", "abc123", false
                )),
                List.of()
        );

        assertThat(manifest.files()).hasSize(1);
        assertThat(manifest.files().get(0).relativePath()).isEqualTo("tests/login.spec.ts");
    }

    @Test
    @DisplayName("Não deve incluir conteúdo do arquivo (apenas metadados)")
    void naoDeveSerializarConteudo() throws Exception {
        GenerationManifest manifest = new GenerationManifest(
                UUID.randomUUID(), "PLAYWRIGHT", "TYPESCRIPT", "READY", "COMPLETED", "2026-08-05T10:00:00",
                List.of(new GenerationManifest.GenerationManifestFile(
                        "tests/login.spec.ts", "CREATE", "TEST", "GENERATED", "abc123", false
                )),
                List.of()
        );

        String json = new ObjectMapper().writeValueAsString(manifest);

        assertThat(json).doesNotContain("content");
        assertThat(json).doesNotContain("projectPath");
    }

    @Test
    @DisplayName("Deve retornar coleções imutáveis")
    void deveRetornarColecoesImutaveis() {
        GenerationManifest manifest = new GenerationManifest(
                UUID.randomUUID(), "PLAYWRIGHT", "TYPESCRIPT", "READY", "COMPLETED", "2026-08-05T10:00:00",
                List.of(), List.of()
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> manifest.files().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
