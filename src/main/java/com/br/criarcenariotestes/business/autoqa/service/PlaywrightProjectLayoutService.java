package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.context.AutomationPlan;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedCodeResponse;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectDiscoveryResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PlaywrightProjectLayoutService {

    private static final Pattern TEST_DIR_PATTERN = Pattern.compile("testDir\\s*:\\s*['\"`]([^'\"`]+)['\"`]");
    private static final List<String> TEST_SUFFIXES = List.of(".spec.ts", ".spec.js", ".test.ts", ".test.js");
    private static final List<String> DEFAULT_PAGE_DIR_CANDIDATES = List.of("pages", "page-objects", "pageObjects");

    public String resolvePreferredTestDirectory(
            Path projectPath,
            ProjectDiscoveryResult discoveryResult,
            ProjectAnalysisResult analysis
    ) {
        String fromConfig = readTestDirFromConfig(projectPath, discoveryResult);
        if (fromConfig != null) {
            return fromConfig;
        }

        if (analysis != null && analysis.getTestFiles() != null) {
            for (String testFile : analysis.getTestFiles()) {
                String normalized = normalizeRelative(testFile);
                int slash = normalized.indexOf('/');
                if (slash > 0) {
                    return normalized.substring(0, slash);
                }
            }
        }
        return "tests";
    }

    public String resolvePreferredPageObjectDirectory(
            Path projectPath,
            ProjectAnalysisResult analysis,
            String preferredTestDir
    ) {
        if (analysis != null && analysis.getPageObjects() != null) {
            for (var pageObject : analysis.getPageObjects()) {
                String sourceFile = pageObject.getSourceFile();
                if (sourceFile == null || sourceFile.isBlank()) {
                    continue;
                }
                String normalized = normalizeRelative(sourceFile);
                int slash = normalized.lastIndexOf('/');
                if (slash > 0) {
                    return normalized.substring(0, slash);
                }
            }
        }

        for (String candidate : DEFAULT_PAGE_DIR_CANDIDATES) {
            if (Files.isDirectory(projectPath.resolve(candidate))) {
                return candidate;
            }
        }
        return preferredTestDir + "/pages";
    }

    public GeneratedCodeResponse normalizeGeneratedPaths(
            GeneratedCodeResponse generated,
            String preferredTestDir,
            String preferredPageObjectDir
    ) {
        if (generated == null || generated.files() == null || generated.files().isEmpty()) {
            return generated;
        }

        String testDir = normalizeRelative(preferredTestDir);
        String pageDir = normalizeRelative(preferredPageObjectDir);
        List<GeneratedFile> normalizedFiles = new ArrayList<>();

        for (GeneratedFile file : generated.files()) {
            if (file == null || file.relativePath() == null) {
                continue;
            }
            String normalizedPath = normalizeRelative(file.relativePath());
            normalizedPath = rewriteTestPath(normalizedPath, testDir);
            normalizedPath = rewritePageObjectPath(normalizedPath, pageDir);

            normalizedFiles.add(new GeneratedFile(
                    normalizedPath,
                    file.operation(),
                    file.content(),
                    file.explanation(),
                    file.generatedHash()
            ));
        }

        return new GeneratedCodeResponse(
                normalizedFiles,
                generated.reusedComponents(),
                generated.missingComponents(),
                generated.warnings(),
                generated.summary(),
                generated.generationFailed(),
                generated.failureReason()
        );
    }

    public boolean hasPageObjectFile(GeneratedCodeResponse generated) {
        if (generated == null || generated.files() == null) {
            return false;
        }
        return generated.files().stream()
                .filter(f -> f != null && f.relativePath() != null)
                .map(f -> normalizeRelative(f.relativePath()).toLowerCase(Locale.ROOT))
                .anyMatch(path -> path.contains("/pages/")
                        || path.contains("/page-objects/")
                        || path.endsWith("page.ts")
                        || path.endsWith("page.js")
                        || path.endsWith("pageobject.ts")
                        || path.endsWith("pageobject.js"));
    }

    private String readTestDirFromConfig(Path projectPath, ProjectDiscoveryResult discoveryResult) {
        if (discoveryResult == null || discoveryResult.getConfigurationFile() == null) {
            return null;
        }
        Path configPath = projectPath.resolve(discoveryResult.getConfigurationFile());
        if (!Files.exists(configPath)) {
            return null;
        }
        try {
            String content = Files.readString(configPath);
            Matcher matcher = TEST_DIR_PATTERN.matcher(content);
            if (matcher.find()) {
                String value = normalizeRelative(matcher.group(1));
                return value.isBlank() ? null : value;
            }
        } catch (IOException ignored) {
            return null;
        }
        return null;
    }

    private String rewriteTestPath(String relativePath, String testDir) {
        String lower = relativePath.toLowerCase(Locale.ROOT);
        if (!isTestFile(lower)) {
            return relativePath;
        }
        if (lower.startsWith("tests/")) {
            return testDir + "/" + relativePath.substring("tests/".length());
        }
        if (lower.startsWith("e2e/")) {
            return testDir + "/" + relativePath.substring("e2e/".length());
        }
        if (!relativePath.contains("/")) {
            return testDir + "/" + relativePath;
        }
        return relativePath;
    }

    private String rewritePageObjectPath(String relativePath, String preferredPageObjectDir) {
        String lower = relativePath.toLowerCase(Locale.ROOT);
        if (lower.startsWith("tests/pages/")) {
            return preferredPageObjectDir + "/" + relativePath.substring("tests/pages/".length());
        }
        if (lower.startsWith("e2e/pages/")) {
            return preferredPageObjectDir + "/" + relativePath.substring("e2e/pages/".length());
        }
        return relativePath;
    }

    private boolean isTestFile(String lowerRelativePath) {
        return TEST_SUFFIXES.stream().anyMatch(lowerRelativePath::endsWith);
    }

    private String normalizeRelative(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.replace("\\", "/").trim();
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
