package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.context.ProjectCatalog;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectCatalogEntry;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Escaneia o projeto de automação e produz um catálogo de arquivos.
 * Não usa IA — análise completamente determinística.
 * Respeita limites configurados, exclui arquivos sensíveis e binários.
 */
@Service
@RequiredArgsConstructor
public class ProjectScannerService {

    private static final Logger log = LoggerFactory.getLogger(ProjectScannerService.class);

    private static final Set<String> ALWAYS_IGNORED_DIRS = Set.of(
            "node_modules", ".git", "dist", "build", "target", "out",
            "coverage", "playwright-report", "test-results", "blob-report",
            "allure-results", "allure-report",
            ".idea", ".vscode", ".gradle", "logs"
    );

    private static final Set<String> SENSITIVE_FILE_PATTERNS = Set.of(
            ".env", ".env.local", ".env.development", ".env.test",
            ".env.production", ".env.staging",
            "id_rsa", "id_ed25519", "id_dsa"
    );

    private static final Set<String> SENSITIVE_EXTENSIONS = Set.of(
            ".pem", ".key", ".p12", ".pfx", ".crt", ".cer"
    );

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico", ".bmp", ".webp",
            ".pdf", ".zip", ".tar", ".gz", ".bz2", ".7z", ".rar",
            ".jar", ".class", ".exe", ".dll", ".so", ".dylib",
            ".woff", ".woff2", ".ttf", ".eot", ".otf",
            ".mp4", ".mp3", ".avi", ".mov", ".wav"
    );

    private static final Set<String> PRIORITIZED_FILES = Set.of(
            "package.json", "playwright.config.ts", "playwright.config.js",
            "playwright.config.mts", "playwright.config.mjs",
            "cypress.config.ts", "cypress.config.js",
            "tsconfig.json", "tsconfig.base.json",
            "README.md", ".npmrc"
    );

    private final AutoQaProperties properties;

    public ProjectCatalog scan(Path projectPath, List<String> additionalIgnoredDirs) {
        log.info("Iniciando escaneamento. path='{}', maxFiles={}, maxFileSizeKb={}",
                projectPath, properties.getMaxFiles(), properties.getMaxFileSizeKb());

        Set<String> ignoredDirs = buildIgnoredSet(additionalIgnoredDirs);
        List<ProjectCatalogEntry> entries = new ArrayList<>();
        List<String> ignoredPaths = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        long[] totalBytes = {0};
        int[] fileCount = {0};
        boolean[] limitReached = {false};

        try {
            Files.walkFileTree(projectPath, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(projectPath)) return FileVisitResult.CONTINUE;
                    String dirName = dir.getFileName().toString();
                    String relativeDir = projectPath.relativize(dir).toString().replace("\\", "/");

                    if (shouldIgnoreDirectory(dirName, relativeDir, ignoredDirs)) {
                        ignoredPaths.add(relativeDir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (limitReached[0]) return FileVisitResult.CONTINUE;

                    String fileName = file.getFileName().toString();
                    String relativePath = projectPath.relativize(file).toString().replace("\\", "/");
                    String ext = extension(fileName);

                    if (isSensitiveFile(fileName, ext)) {
                        ignoredPaths.add(relativePath);
                        return FileVisitResult.CONTINUE;
                    }

                    if (isBinaryExtension(ext)) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (fileCount[0] >= properties.getMaxFiles()) {
                        if (!limitReached[0]) {
                            warnings.add("Limite de arquivos atingido (" + properties.getMaxFiles()
                                    + "). Arquivos adicionais foram ignorados");
                            limitReached[0] = true;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    long sizeBytes = attrs.size();
                    long sizeKb = sizeBytes / 1024;
                    boolean prioritized = PRIORITIZED_FILES.contains(fileName);

                    String content = null;
                    boolean contentLoaded = false;

                    if (sizeKb > properties.getMaxFileSizeKb()) {
                        warnings.add("Arquivo ignorado por exceder o limite de tamanho ("
                                + sizeKb + " KB / " + properties.getMaxFileSizeKb() + " KB): " + relativePath);
                    } else if (isTextFile(ext) || prioritized) {
                        long contentKb = totalBytes[0] / 1024;
                        if (contentKb < properties.getMaxTotalContentKb()) {
                            try {
                                content = Files.readString(file);
                                contentLoaded = true;
                                totalBytes[0] += sizeBytes;
                            } catch (IOException ex) {
                                warnings.add("Não foi possível ler o arquivo: " + relativePath);
                            }
                        }
                    }

                    entries.add(new ProjectCatalogEntry(relativePath, sizeBytes, contentLoaded, content, prioritized));
                    fileCount[0]++;
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            log.error("Erro ao escanear projeto: {}", ex.getMessage(), ex);
            warnings.add("Erro durante escaneamento: " + ex.getMessage());
        }

        log.info("Escaneamento concluído. arquivos={}, totalKb={}, ignorados={}",
                entries.size(), totalBytes[0] / 1024, ignoredPaths.size());

        return ProjectCatalog.builder()
                .projectRoot(projectPath)
                .entries(entries)
                .totalFilesScanned(fileCount[0])
                .totalContentBytes(totalBytes[0])
                .ignoredPaths(ignoredPaths)
                .warnings(warnings)
                .scannedAt(LocalDateTime.now())
                .build();
    }

    private Set<String> buildIgnoredSet(List<String> additionalIgnoredDirs) {
        Set<String> set = new HashSet<>(ALWAYS_IGNORED_DIRS);
        if (additionalIgnoredDirs != null) {
            set.addAll(additionalIgnoredDirs);
        }
        return set;
    }

    private boolean shouldIgnoreDirectory(String dirName, String relativePath, Set<String> ignoredDirs) {
        if (ignoredDirs.contains(dirName)) return true;
        // Suporte para caminhos compostos como "cypress/videos"
        return ignoredDirs.stream().anyMatch(ignored ->
                relativePath.equals(ignored) || relativePath.startsWith(ignored + "/")
        );
    }

    private boolean isSensitiveFile(String fileName, String ext) {
        if (SENSITIVE_FILE_PATTERNS.contains(fileName)) return true;
        if (fileName.startsWith(".env")) return true;
        return SENSITIVE_EXTENSIONS.contains(ext);
    }

    private boolean isBinaryExtension(String ext) {
        return BINARY_EXTENSIONS.contains(ext);
    }

    private boolean isTextFile(String ext) {
        return Set.of(".ts", ".tsx", ".js", ".jsx", ".mts", ".mjs",
                ".json", ".md", ".yaml", ".yml", ".toml", ".txt",
                ".html", ".css", ".scss", ".less", ".xml", ".sh").contains(ext);
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return "";
        return fileName.substring(dot).toLowerCase(Locale.ROOT);
    }
}
