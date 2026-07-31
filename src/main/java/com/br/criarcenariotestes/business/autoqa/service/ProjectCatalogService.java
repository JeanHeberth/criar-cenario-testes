package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.context.ProjectCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectCatalogService {

    private final ProjectScannerService projectScannerService;

    public ProjectCatalog buildCatalog(Path projectPath, List<String> ignoredDirectories) {
        return projectScannerService.scan(projectPath, ignoredDirectories);
    }
}
