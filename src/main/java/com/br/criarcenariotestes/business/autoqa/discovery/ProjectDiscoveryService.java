package com.br.criarcenariotestes.business.autoqa.discovery;

import com.br.criarcenariotestes.business.autoqa.discovery.builder.ProjectDiscoveryResultBuilder;
import com.br.criarcenariotestes.business.autoqa.discovery.detector.FrameworkDetection;
import com.br.criarcenariotestes.business.autoqa.discovery.detector.FrameworkDetector;
import com.br.criarcenariotestes.business.autoqa.discovery.parser.ParsedProjectFiles;
import com.br.criarcenariotestes.business.autoqa.discovery.parser.ProjectFilesParser;
import com.br.criarcenariotestes.business.autoqa.discovery.scanner.ProjectScanResult;
import com.br.criarcenariotestes.business.autoqa.discovery.scanner.ProjectScanner;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.security.ProjectPathSecurityValidator;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Service
public class ProjectDiscoveryService {

    private final ProjectScanner projectScanner;
    private final ProjectFilesParser projectFilesParser;
    private final List<FrameworkDetector> frameworkDetectors;
    private final ProjectDiscoveryResultBuilder resultBuilder;
    private final ProjectPathSecurityValidator projectPathSecurityValidator;

    public ProjectDiscoveryService(ProjectScanner projectScanner,
                                   ProjectFilesParser projectFilesParser,
                                   List<FrameworkDetector> frameworkDetectors,
                                   ProjectDiscoveryResultBuilder resultBuilder,
                                   ProjectPathSecurityValidator projectPathSecurityValidator) {
        this.projectScanner = Objects.requireNonNull(projectScanner, "projectScanner must not be null");
        this.projectFilesParser = Objects.requireNonNull(projectFilesParser, "projectFilesParser must not be null");
        this.frameworkDetectors = List.copyOf(Objects.requireNonNull(frameworkDetectors, "frameworkDetectors must not be null"));
        this.resultBuilder = Objects.requireNonNull(resultBuilder, "resultBuilder must not be null");
        this.projectPathSecurityValidator = Objects.requireNonNull(projectPathSecurityValidator, "projectPathSecurityValidator must not be null");
    }

    public ProjectDiscoveryResult discover(Path projectPath) {
        // Validação autoritativa: mesmo que AutoQaExecutionOrchestrator.create() já
        // tenha validado o path na criação, Discovery nunca confia cegamente nisso
        // (defesa em profundidade — documentos reidratados do Mongo, execuções
        // antigas anteriores a este hardening, ou filesystem que mudou entre a
        // criação e a execução real). Mesma política central, nunca duplicada.
        Path normalizedProjectPath = projectPathSecurityValidator.validate(projectPath);
        ProjectScanResult scanResult = projectScanner.scan(normalizedProjectPath);
        ParsedProjectFiles parsedProjectFiles = projectFilesParser.parse(scanResult);
        List<FrameworkDetection> detections = frameworkDetectors.stream()
                .map(detector -> detector.detect(parsedProjectFiles))
                .filter(FrameworkDetection::detected)
                .toList();
        return resultBuilder.build(normalizedProjectPath, parsedProjectFiles, detections);
    }
}
