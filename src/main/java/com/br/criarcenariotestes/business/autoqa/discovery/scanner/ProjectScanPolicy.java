package com.br.criarcenariotestes.business.autoqa.discovery.scanner;

import java.util.Set;

public class ProjectScanPolicy {

    private static final int MAX_SCAN_DEPTH = 4;
    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            "target",
            "out",
            "coverage",
            "playwright-report",
            "test-results",
            "blob-report",
            "allure-results",
            "allure-report",
            ".idea",
            ".vscode",
            ".gradle",
            "logs"
    );

    public int maxDepth() {
        return MAX_SCAN_DEPTH;
    }

    public boolean isIgnoredDirectory(String name) {
        return IGNORED_DIRECTORIES.contains(name);
    }

    public boolean isIgnoredFile(String name) {
        String lower = name.toLowerCase();
        return lower.equals(".env")
                || lower.startsWith(".env.")
                || lower.endsWith(".pem")
                || lower.endsWith(".key")
                || lower.endsWith(".crt")
                || lower.endsWith(".cer")
                || lower.endsWith(".p12")
                || lower.endsWith(".pfx")
                || lower.endsWith(".zip")
                || lower.endsWith(".tar")
                || lower.endsWith(".gz")
                || lower.endsWith(".jar")
                || lower.endsWith(".war")
                || lower.endsWith(".class");
    }
}
