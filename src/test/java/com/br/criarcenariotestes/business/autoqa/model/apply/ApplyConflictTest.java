package com.br.criarcenariotestes.business.autoqa.model.apply;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ApplyConflict - Testes Unitários")
class ApplyConflictTest {

    @Test
    @DisplayName("Deve criar conflito válido")
    void deveCriarConflitoValido() {
        ApplyConflict conflict = new ApplyConflict("src/Foo.java", ApplyConflict.TARGET_ALREADY_EXISTS, "já existe");

        assertThat(conflict.relativePath()).isEqualTo("src/Foo.java");
        assertThat(conflict.type()).isEqualTo("TARGET_ALREADY_EXISTS");
        assertThat(conflict.message()).isEqualTo("já existe");
    }

    @Test
    @DisplayName("Deve permitir relativePath nulo para conflito global de manifest")
    void devePermitirRelativePathNuloParaConflitoGlobal() {
        ApplyConflict conflict = new ApplyConflict(null, ApplyConflict.MANIFEST_MISMATCH, "executionId divergente");

        assertThat(conflict.relativePath()).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar type nulo")
    void deveRejeitarTypeNulo() {
        assertThatThrownBy(() -> new ApplyConflict("src/Foo.java", null, "msg"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar type em branco")
    void deveRejeitarTypeEmBranco() {
        assertThatThrownBy(() -> new ApplyConflict("src/Foo.java", "   ", "msg"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar message nula")
    void deveRejeitarMessageNula() {
        assertThatThrownBy(() -> new ApplyConflict("src/Foo.java", ApplyConflict.TARGET_MISSING, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve expor todos os tipos de conflito conhecidos")
    void deveExporTodosOsTiposConhecidos() {
        assertThat(ApplyConflict.TARGET_ALREADY_EXISTS).isEqualTo("TARGET_ALREADY_EXISTS");
        assertThat(ApplyConflict.TARGET_MISSING).isEqualTo("TARGET_MISSING");
        assertThat(ApplyConflict.ORIGINAL_FILE_CHANGED).isEqualTo("ORIGINAL_FILE_CHANGED");
        assertThat(ApplyConflict.GENERATED_HASH_MISMATCH).isEqualTo("GENERATED_HASH_MISMATCH");
        assertThat(ApplyConflict.MANIFEST_MISMATCH).isEqualTo("MANIFEST_MISMATCH");
        assertThat(ApplyConflict.PATH_SECURITY_VIOLATION).isEqualTo("PATH_SECURITY_VIOLATION");
    }
}
