package com.br.criarcenariotestes.business.autoqa.knowledge.scanner;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class KnowledgeScanPolicy {

    public static final int DEFAULT_MAX_DEPTH = 8;
    public static final int DEFAULT_MAX_FILES = 250;
    public static final long DEFAULT_MAX_FILE_BYTES = 32_768L;
    public static final long DEFAULT_MAX_TOTAL_BYTES = 262_144L;

    private final int maxDepth;
    private final int maxFiles;
    private final long maxFileBytes;
    private final long maxTotalBytes;
    private final Set<String> allowedExtensions;
    private final Set<String> ignoredDirectories;
    private final Set<String> ignoredExactNames;
    private final Set<String> ignoredPrefixes;
    private final Set<String> ignoredSuffixes;

    public KnowledgeScanPolicy() {
        this(
                DEFAULT_MAX_DEPTH,
                DEFAULT_MAX_FILES,
                DEFAULT_MAX_FILE_BYTES,
                DEFAULT_MAX_TOTAL_BYTES,
                Set.of(".ts", ".tsx", ".js", ".jsx", ".java", ".py", ".robot", ".resource", ".json", ".yaml", ".yml", ".properties"),
                Set.of("node_modules", ".git", "dist", "build", "target", "out", "coverage", "playwright-report", "test-results", "allure-results", "allure-report", ".idea", ".vscode", ".gradle", "logs", "backups", ".auto-qa"),
                Set.of(".env", "package-lock.json", "yarn.lock", "pnpm-lock.yaml", "poetry.lock"),
                Set.of(".env."),
                Set.of(".pem", ".key", ".crt", ".cer", ".p12", ".pfx", ".jks", ".keystore", ".mp4", ".mov", ".avi", ".mkv", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".zip", ".tar", ".gz", ".7z", ".rar", ".pdf")
        );
    }

    public KnowledgeScanPolicy(int maxDepth,
                               int maxFiles,
                               long maxFileBytes,
                               long maxTotalBytes,
                               Set<String> allowedExtensions,
                               Set<String> ignoredDirectories,
                               Set<String> ignoredExactNames,
                               Set<String> ignoredPrefixes,
                               Set<String> ignoredSuffixes) {
        this.maxDepth = maxDepth;
        this.maxFiles = maxFiles;
        this.maxFileBytes = maxFileBytes;
        this.maxTotalBytes = maxTotalBytes;
        this.allowedExtensions = Set.copyOf(allowedExtensions);
        this.ignoredDirectories = Set.copyOf(ignoredDirectories);
        this.ignoredExactNames = Set.copyOf(ignoredExactNames);
        this.ignoredPrefixes = Set.copyOf(ignoredPrefixes);
        this.ignoredSuffixes = Set.copyOf(ignoredSuffixes);
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    public long getMaxFileBytes() {
        return maxFileBytes;
    }

    public long getMaxTotalBytes() {
        return maxTotalBytes;
    }

    public Set<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public Set<String> getIgnoredDirectories() {
        return ignoredDirectories;
    }

    public Set<String> getIgnoredExactNames() {
        return ignoredExactNames;
    }

    public Set<String> getIgnoredPrefixes() {
        return ignoredPrefixes;
    }

    public Set<String> getIgnoredSuffixes() {
        return ignoredSuffixes;
    }

    public boolean isAllowedExtension(String fileName) {
        String lower = fileName.toLowerCase();
        return allowedExtensions.stream().anyMatch(lower::endsWith);
    }

    public boolean isIgnoredFile(String fileName) {
        String lower = fileName.toLowerCase();
        if (ignoredExactNames.contains(lower)) {
            return true;
        }
        for (String prefix : ignoredPrefixes) {
            if (lower.startsWith(prefix)) {
                return true;
            }
        }
        for (String suffix : ignoredSuffixes) {
            if (lower.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    public boolean isIgnoredDirectory(String directoryName) {
        return ignoredDirectories.contains(directoryName.toLowerCase());
    }
}
