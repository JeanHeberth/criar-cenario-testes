package com.br.criarcenariotestes.business.autoqa.model.apply;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BackupRecord - Testes Unitários")
class BackupRecordTest {

    @Test
    @DisplayName("Deve criar backup record válido")
    void deveCriarBackupRecordValido() {
        LocalDateTime now = LocalDateTime.now();
        BackupRecord record = new BackupRecord("src/Foo.java", "files/src/Foo.java", "abc123", now);

        assertThat(record.relativePath()).isEqualTo("src/Foo.java");
        assertThat(record.backupRelativePath()).isEqualTo("files/src/Foo.java");
        assertThat(record.sha256()).isEqualTo("abc123");
        assertThat(record.backedUpAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Deve rejeitar relativePath nulo")
    void deveRejeitarRelativePathNulo() {
        assertThatThrownBy(() -> new BackupRecord(null, "files/x", "hash", LocalDateTime.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar relativePath em branco")
    void deveRejeitarRelativePathEmBranco() {
        assertThatThrownBy(() -> new BackupRecord("   ", "files/x", "hash", LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar backupRelativePath nulo")
    void deveRejeitarBackupRelativePathNulo() {
        assertThatThrownBy(() -> new BackupRecord("src/Foo.java", null, "hash", LocalDateTime.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar sha256 nulo")
    void deveRejeitarSha256Nulo() {
        assertThatThrownBy(() -> new BackupRecord("src/Foo.java", "files/x", null, LocalDateTime.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar backedUpAt nulo")
    void deveRejeitarBackedUpAtNulo() {
        assertThatThrownBy(() -> new BackupRecord("src/Foo.java", "files/x", "hash", null))
                .isInstanceOf(NullPointerException.class);
    }
}
