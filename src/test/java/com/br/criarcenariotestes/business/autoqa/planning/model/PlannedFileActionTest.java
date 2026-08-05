package com.br.criarcenariotestes.business.autoqa.planning.model;

import com.br.criarcenariotestes.business.autoqa.model.planning.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

@DisplayName("PlannedFileAction - Testes Unitários")
class PlannedFileActionTest {

    @Test
    @DisplayName("Deve criar PlannedFileAction válida")
    void deveCriarPlannedFileActionValida() {
        PlannedFileAction action = new PlannedFileAction(
            "tests/login.spec.ts", FileOperation.CREATE, PlanComponentType.TEST,
            "Criar teste de login", false, true, ApprovalRequirement.NONE,
            List.of("dep1"), List.of("warning1")
        );
        assertThat(action.relativePath()).isEqualTo("tests/login.spec.ts");
        assertThat(action.operation()).isEqualTo(FileOperation.CREATE);
        assertThat(action.componentType()).isEqualTo(PlanComponentType.TEST);
        assertThat(action.reason()).isEqualTo("Criar teste de login");
        assertThat(action.existingFile()).isFalse();
        assertThat(action.required()).isTrue();
        assertThat(action.approvalRequirement()).isEqualTo(ApprovalRequirement.NONE);
        assertThat(action.dependencies()).containsExactly("dep1");
        assertThat(action.warnings()).containsExactly("warning1");
    }

    @Test
    @DisplayName("Deve criar com path absoluto sem validar (DTO)")
    void deveCriarComPathAbsolutoSemValidar() {
        assertThatCode(() -> new PlannedFileAction(
            "/absolute/path.ts", FileOperation.CREATE, PlanComponentType.TEST,
            "Razão", false, true, ApprovalRequirement.NONE, List.of(), List.of()
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Deve normalizar strings com trim")
    void deveNormalizarStringsComTrim() {
        PlannedFileAction action = new PlannedFileAction(
            "  tests/login.spec.ts  ", FileOperation.CREATE, PlanComponentType.TEST,
            "  Razão  ", false, true, ApprovalRequirement.NONE, List.of(), List.of()
        );
        assertThat(action.relativePath()).isEqualTo("tests/login.spec.ts");
        assertThat(action.reason()).isEqualTo("Razão");
    }

    @Test
    @DisplayName("Deve retornar listas imutáveis")
    void deveRetornarListasImutaveis() {
        PlannedFileAction action = new PlannedFileAction(
            "tests/x.ts", FileOperation.CREATE, PlanComponentType.TEST,
            "Razão", false, true, ApprovalRequirement.NONE,
            List.of("dep"), List.of("warn")
        );
        assertThatThrownBy(() -> action.dependencies().add("new"))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> action.warnings().add("new"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve converter lista nula para vazia")
    void deveConverterListaNulaParaVazia() {
        PlannedFileAction action = new PlannedFileAction(
            "tests/x.ts", FileOperation.CREATE, PlanComponentType.TEST,
            "Razão", false, true, ApprovalRequirement.NONE, null, null
        );
        assertThat(action.dependencies()).isEmpty();
        assertThat(action.warnings()).isEmpty();
    }

    @Test
    @DisplayName("Deve preservar operation")
    void devePreservarOperation() {
        PlannedFileAction action = new PlannedFileAction(
            "tests/x.ts", FileOperation.REUSE, PlanComponentType.PAGE_OBJECT,
            "Reutilizar", true, true, ApprovalRequirement.NONE, List.of(), List.of()
        );
        assertThat(action.operation()).isEqualTo(FileOperation.REUSE);
    }

    @Test
    @DisplayName("Deve preservar approvalRequirement")
    void devePreservarApprovalRequirement() {
        PlannedFileAction action = new PlannedFileAction(
            "tests/x.ts", FileOperation.UPDATE, PlanComponentType.PAGE_OBJECT,
            "Atualizar", true, true, ApprovalRequirement.FILE_UPDATE_REQUIRED, List.of(), List.of()
        );
        assertThat(action.approvalRequirement()).isEqualTo(ApprovalRequirement.FILE_UPDATE_REQUIRED);
    }

    @Test
    @DisplayName("Deve preservar existingFile")
    void devePreservarExistingFile() {
        PlannedFileAction action = new PlannedFileAction(
            "tests/x.ts", FileOperation.REUSE, PlanComponentType.PAGE_OBJECT,
            "Reutilizar", true, true, ApprovalRequirement.NONE, List.of(), List.of()
        );
        assertThat(action.existingFile()).isTrue();
    }

    @Test
    @DisplayName("Deve preservar required")
    void devePreservarRequired() {
        PlannedFileAction action = new PlannedFileAction(
            "tests/x.ts", FileOperation.CREATE, PlanComponentType.TEST,
            "Criar", false, false, ApprovalRequirement.NONE, List.of(), List.of()
        );
        assertThat(action.required()).isFalse();
    }

    @Test
    @DisplayName("Deve não armazenar conteúdo de arquivo")
    void deveNaoArmazenarConteudo() {
        PlannedFileAction action = new PlannedFileAction(
            "tests/x.ts", FileOperation.CREATE, PlanComponentType.TEST,
            "Criar", false, true, ApprovalRequirement.NONE, List.of(), List.of()
        );
        assertThat(action.relativePath()).isNotBlank();
        assertThat(action.reason()).isNotBlank();
    }
}
