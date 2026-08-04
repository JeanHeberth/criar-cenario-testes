package com.br.criarcenariotestes.business.autoqa.discovery;

import com.br.criarcenariotestes.business.autoqa.discovery.builder.ProjectDiscoveryResultBuilder;
import com.br.criarcenariotestes.business.autoqa.discovery.detector.FrameworkDetection;
import com.br.criarcenariotestes.business.autoqa.discovery.detector.FrameworkDetector;
import com.br.criarcenariotestes.business.autoqa.discovery.parser.ParsedProjectFiles;
import com.br.criarcenariotestes.business.autoqa.discovery.parser.ProjectFilesParser;
import com.br.criarcenariotestes.business.autoqa.discovery.scanner.ProjectScanResult;
import com.br.criarcenariotestes.business.autoqa.discovery.scanner.ProjectScanner;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Service
public class ProjectDiscoveryService {

    private final ProjectScanner projectScanner;
    private final ProjectFilesParser projectFilesParser;
    private final List<FrameworkDetector> frameworkDetectors;
    private final ProjectDiscoveryResultBuilder resultBuilder;

    public ProjectDiscoveryService(ProjectScanner projectScanner,
                                   ProjectFilesParser projectFilesParser,
                                   List<FrameworkDetector> frameworkDetectors,
                                   ProjectDiscoveryResultBuilder resultBuilder) {
        this.projectScanner = Objects.requireNonNull(projectScanner, "projectScanner must not be null");
        this.projectFilesParser = Objects.requireNonNull(projectFilesParser, "projectFilesParser must not be null");
        this.frameworkDetectors = List.copyOf(Objects.requireNonNull(frameworkDetectors, "frameworkDetectors must not be null"));
        this.resultBuilder = Objects.requireNonNull(resultBuilder, "resultBuilder must not be null");
    }

    public ProjectDiscoveryResult discover(Path projectPath) {
        Path normalizedProjectPath = normalizeAndValidate(projectPath);
        ProjectScanResult scanResult = projectScanner.scan(normalizedProjectPath);
        ParsedProjectFiles parsedProjectFiles = projectFilesParser.parse(scanResult);
        List<FrameworkDetection> detections = frameworkDetectors.stream()
                .map(detector -> detector.detect(parsedProjectFiles))
                .filter(FrameworkDetection::detected)
                .toList();
        return resultBuilder.build(normalizedProjectPath, parsedProjectFiles, detections);
    }

    private Path normalizeAndValidate(Path projectPath) {
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        if (projectPath.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("projectPath must not be blank");
        }

        Path normalized = projectPath.toAbsolutePath().normalize();
        if (!Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("projectPath does not exist");
        }
        if (!Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("projectPath must be a directory");
        }
        if (!Files.isReadable(normalized)) {
            throw new IllegalArgumentException("projectPath must be readable");
        }
        return normalized;
    }
}
