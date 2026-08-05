package com.br.criarcenariotestes.business.autoqa.knowledge.scanner;

import com.br.criarcenariotestes.business.autoqa.knowledge.ProjectKnowledgeScanException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class KnowledgeScanner {

    private final KnowledgeScanPolicy policy;

    public KnowledgeScanner(KnowledgeScanPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    public KnowledgeScanResult scan(Path normalizedProjectPath) {
        Objects.requireNonNull(normalizedProjectPath, "normalizedProjectPath must not be null");

        try {
            List<Path> candidates;
            try (var stream = Files.walk(normalizedProjectPath, policy.getMaxDepth())) {
                candidates = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> !Files.isSymbolicLink(path))
                        .filter(path -> isUnderRoot(normalizedProjectPath, path))
                        .sorted(Comparator.comparing(path -> toRelativeString(normalizedProjectPath, path)))
                        .toList();
            }

            List<KnowledgeScanResult.KnowledgeFile> files = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List<String> ignoredPaths = new ArrayList<>();
            long totalBytes = 0L;
            for (Path path : candidates) {
                String relativePath = toRelativeString(normalizedProjectPath, path);
                String fileName = path.getFileName().toString();
                if (isIgnored(normalizedProjectPath, path, relativePath, fileName)) {
                    ignoredPaths.add(relativePath);
                    continue;
                }
                if (!policy.isAllowedExtension(fileName)) {
                    ignoredPaths.add(relativePath);
                    continue;
                }
                long size = Files.size(path);
                if (size > policy.getMaxFileBytes()) {
                    warnings.add("Arquivo acima do limite: " + relativePath);
                    continue;
                }
                if (files.size() >= policy.getMaxFiles()) {
                    warnings.add("Limite de arquivos atingido");
                    break;
                }
                if (totalBytes + size > policy.getMaxTotalBytes()) {
                    warnings.add("Limite total de leitura atingido");
                    break;
                }
                String content = readContent(path);
                files.add(new KnowledgeScanResult.KnowledgeFile(relativePath, fileName, extension(fileName), size, content));
                totalBytes += size;
            }
            return new KnowledgeScanResult(normalizedProjectPath, files, distinctSorted(ignoredPaths), distinctSorted(warnings), false);
        } catch (IOException exception) {
            throw new ProjectKnowledgeScanException("Falha ao escanear projeto", exception);
        }
    }

    private boolean isIgnored(Path root, Path path, String relativePath, String fileName) {
        if (policy.isIgnoredFile(fileName)) {
            return true;
        }
        Path relative = root.relativize(path);
        for (Path part : relative) {
            if (policy.isIgnoredDirectory(part.toString().toLowerCase())) {
                return true;
            }
        }
        return relativePath.contains("/.auto-qa/") || relativePath.startsWith(".auto-qa/");
    }

    private boolean isUnderRoot(Path root, Path path) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedPath = path.toAbsolutePath().normalize();
        return normalizedPath.startsWith(normalizedRoot);
    }

    private String toRelativeString(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private String readContent(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index >= 0 ? fileName.substring(index).toLowerCase() : "";
    }

    private List<String> distinctSorted(List<String> values) {
        return values.stream().distinct().sorted().toList();
    }
}
