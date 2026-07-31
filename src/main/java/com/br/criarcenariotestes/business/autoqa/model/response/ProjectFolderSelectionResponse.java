package com.br.criarcenariotestes.business.autoqa.model.response;

public record ProjectFolderSelectionResponse(
        boolean selected,
        boolean cancelled,
        String projectPath,
        ProjectValidationResponse validation
) {

    public static ProjectFolderSelectionResponse cancelledSelection() {
        return new ProjectFolderSelectionResponse(false, true, null, null);
    }

    public static ProjectFolderSelectionResponse selectedFolder(String projectPath, ProjectValidationResponse validation) {
        return new ProjectFolderSelectionResponse(true, false, projectPath, validation);
    }
}
