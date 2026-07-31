package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.response.ProjectFolderSelectionResponse;
import com.br.criarcenariotestes.business.autoqa.model.response.ProjectValidationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectFolderSelectionService {

    private final DirectoryChooserService directoryChooserService;
    private final ProjectPathValidationService pathValidationService;

    public ProjectFolderSelectionResponse selectFolderAndValidate() {
        Optional<Path> selectedPath = directoryChooserService.chooseDirectory();
        if (selectedPath.isEmpty()) {
            return ProjectFolderSelectionResponse.cancelledSelection();
        }

        String resolvedPath = selectedPath.get().toAbsolutePath().normalize().toString();
        ProjectValidationResponse validation = pathValidationService.validate(resolvedPath);
        String normalizedPath = validation.normalizedPath() != null
                ? validation.normalizedPath()
                : resolvedPath;

        return ProjectFolderSelectionResponse.selectedFolder(normalizedPath, validation);
    }
}
