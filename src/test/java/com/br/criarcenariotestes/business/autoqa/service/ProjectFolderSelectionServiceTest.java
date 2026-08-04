package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.response.ProjectFolderSelectionResponse;
import com.br.criarcenariotestes.business.autoqa.model.response.ProjectValidationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectFolderSelectionService")
class ProjectFolderSelectionServiceTest {

    @Mock
    private DirectoryChooserService directoryChooserService;

    @Mock
    private ProjectPathValidationService pathValidationService;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("deve retornar seleção bem-sucedida com validação")
    void shouldReturnSuccessfulSelection() {
        Path selectedPath = tempDir.resolve("automation");
        ProjectValidationResponse validation = ProjectValidationResponse.builder()
                .valid(true)
                .normalizedPath(selectedPath.toAbsolutePath().normalize().toString())
                .readable(true)
                .writable(true)
                .detectedFramework(AutomationFramework.PLAYWRIGHT)
                .detectedLanguage(AutomationLanguage.TYPESCRIPT)
                .packageManager(PackageManager.NPM)
                .configurationFile("playwright.config.ts")
                .warnings(List.of())
                .build();
        when(directoryChooserService.chooseDirectory()).thenReturn(Optional.of(selectedPath));
        when(pathValidationService.validate(selectedPath.toAbsolutePath().normalize().toString()))
                .thenReturn(validation);

        ProjectFolderSelectionResponse response = new ProjectFolderSelectionService(
                directoryChooserService, pathValidationService
        ).selectFolderAndValidate();

        assertThat(response.selected()).isTrue();
        assertThat(response.cancelled()).isFalse();
        assertThat(response.projectPath()).isEqualTo(selectedPath.toAbsolutePath().normalize().toString());
        assertThat(response.validation()).isEqualTo(validation);
        verify(pathValidationService).validate(selectedPath.toAbsolutePath().normalize().toString());
    }

    @Test
    @DisplayName("deve retornar cancelamento quando usuário fechar seletor")
    void shouldReturnCancelledWhenUserClosesDialog() {
        when(directoryChooserService.chooseDirectory()).thenReturn(Optional.empty());

        ProjectFolderSelectionResponse response = new ProjectFolderSelectionService(
                directoryChooserService, pathValidationService
        ).selectFolderAndValidate();

        assertThat(response.selected()).isFalse();
        assertThat(response.cancelled()).isTrue();
        assertThat(response.projectPath()).isNull();
        assertThat(response.validation()).isNull();
        verify(pathValidationService, never()).validate(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("deve retornar diretório inválido com detalhes de validação")
    void shouldReturnInvalidDirectoryValidation() {
        Path selectedPath = tempDir.resolve("invalid-folder");
        ProjectValidationResponse invalidValidation = ProjectValidationResponse.invalid("Diretório inválido");
        when(directoryChooserService.chooseDirectory()).thenReturn(Optional.of(selectedPath));
        when(pathValidationService.validate(selectedPath.toAbsolutePath().normalize().toString()))
                .thenReturn(invalidValidation);

        ProjectFolderSelectionResponse response = new ProjectFolderSelectionService(
                directoryChooserService, pathValidationService
        ).selectFolderAndValidate();

        assertThat(response.selected()).isTrue();
        assertThat(response.cancelled()).isFalse();
        assertThat(response.validation()).isNotNull();
        assertThat(response.validation().valid()).isFalse();
        assertThat(response.validation().warnings()).contains("Diretório inválido");
    }

    @Test
    @DisplayName("deve integrar com ProjectPathValidationService usando caminho normalizado")
    void shouldIntegrateUsingNormalizedPath() {
        Path selectedPath = tempDir.resolve("..").resolve(tempDir.getFileName().toString()).resolve("project");
        String normalized = selectedPath.toAbsolutePath().normalize().toString();
        ProjectValidationResponse validation = ProjectValidationResponse.builder()
                .valid(true)
                .normalizedPath(normalized)
                .readable(true)
                .writable(true)
                .detectedFramework(AutomationFramework.UNKNOWN)
                .detectedLanguage(AutomationLanguage.UNKNOWN)
                .packageManager(PackageManager.UNKNOWN)
                .warnings(List.of())
                .build();

        when(directoryChooserService.chooseDirectory()).thenReturn(Optional.of(selectedPath));
        when(pathValidationService.validate(normalized)).thenReturn(validation);

        ProjectFolderSelectionResponse response = new ProjectFolderSelectionService(
                directoryChooserService, pathValidationService
        ).selectFolderAndValidate();

        verify(pathValidationService).validate(normalized);
        assertThat(response.projectPath()).isEqualTo(normalized);
    }
}
