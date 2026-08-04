package com.br.criarcenariotestes.business.autoqa.discovery.parser;

import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.discovery.scanner.ProjectScanResult;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Component
public class ProjectFilesParser {

    private final PackageJsonParser packageJsonParser;
    private final MavenPomParser mavenPomParser;
    private final GradleBuildParser gradleBuildParser;
    private final PythonManifestParser pythonManifestParser;

    public ProjectFilesParser(PackageJsonParser packageJsonParser,
                              MavenPomParser mavenPomParser,
                              GradleBuildParser gradleBuildParser,
                              PythonManifestParser pythonManifestParser) {
        this.packageJsonParser = Objects.requireNonNull(packageJsonParser, "packageJsonParser must not be null");
        this.mavenPomParser = Objects.requireNonNull(mavenPomParser, "mavenPomParser must not be null");
        this.gradleBuildParser = Objects.requireNonNull(gradleBuildParser, "gradleBuildParser must not be null");
        this.pythonManifestParser = Objects.requireNonNull(pythonManifestParser, "pythonManifestParser must not be null");
    }

    public ParsedProjectFiles parse(ProjectScanResult scanResult) {
        ParsedProjectFiles.Builder builder = new ParsedProjectFiles.Builder();
        builder.warnings().addAll(scanResult.warnings());
        builder.evidenceFiles().addAll(scanResult.relativeFiles());

        List<Path> files = scanResult.files();
        List<String> relativeFiles = scanResult.relativeFiles();
        for (int index = 0; index < files.size(); index++) {
            Path file = files.get(index);
            String relativePath = relativeFiles.get(index);
            String lowerName = file.getFileName().toString().toLowerCase();

            try {
                if (lowerName.equals("package.json")) {
                    packageJsonParser.parse(file, relativePath, builder);
                } else if (lowerName.equals("pom.xml")) {
                    mavenPomParser.parse(file, relativePath, builder);
                } else if (lowerName.equals("build.gradle") || lowerName.equals("build.gradle.kts")) {
                    gradleBuildParser.parse(file, relativePath, builder);
                } else if (lowerName.equals("requirements.txt")) {
                    pythonManifestParser.parseRequirements(file, relativePath, builder);
                } else if (lowerName.equals("pyproject.toml")) {
                    pythonManifestParser.parsePyproject(file, relativePath, builder);
                } else if (lowerName.equals("poetry.lock")) {
                    pythonManifestParser.parsePoetryLock(file, relativePath, builder);
                } else if (lowerName.equals("robot.yaml")) {
                    builder.robotYaml(true);
                    builder.robotYamlPath(relativePath);
                } else if (lowerName.equals("tsconfig.json")) {
                    builder.tsconfig(true);
                } else if (lowerName.equals("package-lock.json")) {
                    builder.packageLock(true);
                    builder.packageManagerCandidates().add(PackageManager.NPM);
                } else if (lowerName.equals("yarn.lock")) {
                    builder.yarnLock(true);
                    builder.packageManagerCandidates().add(PackageManager.YARN);
                } else if (lowerName.equals("pnpm-lock.yaml")) {
                    builder.pnpmLock(true);
                    builder.packageManagerCandidates().add(PackageManager.PNPM);
                } else if (isPlaywrightConfig(lowerName)) {
                    builder.playwrightConfig(relativePath);
                    builder.playwrightConfigIsTs(lowerName.endsWith(".ts") || lowerName.endsWith(".mts"));
                } else if (isCypressConfig(lowerName)) {
                    builder.cypressConfig(relativePath);
                    builder.cypressConfigIsTs(lowerName.endsWith(".ts") || lowerName.endsWith(".mts"));
                } else if (lowerName.endsWith(".robot")) {
                    builder.robotFiles().add(relativePath);
                }
            } catch (IOException exception) {
                builder.warnings().add("Falha ao ler arquivo: " + relativePath);
            }
        }
        return builder.build();
    }

    private boolean isPlaywrightConfig(String lowerName) {
        return lowerName.equals("playwright.config.ts")
                || lowerName.equals("playwright.config.js")
                || lowerName.equals("playwright.config.mts")
                || lowerName.equals("playwright.config.mjs");
    }

    private boolean isCypressConfig(String lowerName) {
        return lowerName.equals("cypress.config.ts")
                || lowerName.equals("cypress.config.js")
                || lowerName.equals("cypress.config.mts")
                || lowerName.equals("cypress.config.mjs");
    }
}
