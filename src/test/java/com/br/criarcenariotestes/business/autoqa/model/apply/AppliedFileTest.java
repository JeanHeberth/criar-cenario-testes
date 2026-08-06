package com.br.criarcenariotestes.business.autoqa.model.apply;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AppliedFile - Testes Unitários")
class AppliedFileTest {

    @Test
    @DisplayName("Deve criar AppliedFile válido")
    void deveCriarAppliedFileValido() {
        AppliedFile file = new AppliedFile("src/Foo.java", ApplyOperation.CREATE, ApplyFileStatus.APPLIED,
                null, "hash123", false, null, List.of());

        assertThat(file.relativePath()).isEqualTo("src/Foo.java");
        assertThat(file.operation()).isEqualTo(ApplyOperation.CREATE);
        assertThat(file.status()).isEqualTo(ApplyFileStatus.APPLIED);
        assertThat(file.backupCreated()).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar relativePath nulo")
    void deveRejeitarRelativePathNulo() {
        assertThatThrownBy(() -> new AppliedFile(null, ApplyOperation.CREATE, ApplyFileStatus.APPLIED,
                null, null, false, null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar relativePath em branco")
    void deveRejeitarRelativePathEmBranco() {
        assertThatThrownBy(() -> new AppliedFile("   ", ApplyOperation.CREATE, ApplyFileStatus.APPLIED,
                null, null, false, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar operation nula")
    void deveRejeitarOperationNula() {
        assertThatThrownBy(() -> new AppliedFile("src/Foo.java", null, ApplyFileStatus.APPLIED,
                null, null, false, null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar status nulo")
    void deveRejeitarStatusNulo() {
        assertThatThrownBy(() -> new AppliedFile("src/Foo.java", ApplyOperation.CREATE, null,
                null, null, false, null, List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve tratar warnings nulos como lista vazia e imutável")
    void deveTratarWarningsNulosComoVazia() {
        AppliedFile file = new AppliedFile("src/Foo.java", ApplyOperation.REUSE, ApplyFileStatus.SKIPPED,
                null, null, false, null, null);

        assertThat(file.warnings()).isEmpty();
        assertThatThrownBy(() -> file.warnings().add(new ApplyWarning("C", "D", "INFO", false)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("UPDATE aplicado deve poder registrar backup")
    void deveRegistrarBackupParaUpdate() {
        AppliedFile file = new AppliedFile("src/Foo.java", ApplyOperation.UPDATE, ApplyFileStatus.APPLIED,
                "oldHash", "newHash", true, "files/src/Foo.java", List.of());

        assertThat(file.backupCreated()).isTrue();
        assertThat(file.backupRelativePath()).isEqualTo("files/src/Foo.java");
    }
}
