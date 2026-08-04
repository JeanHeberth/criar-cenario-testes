package com.br.criarcenariotestes.business.autoqa.discovery.scanner;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ProjectScanner {

    private final ProjectScanPolicy policy;

    public ProjectScanner(ProjectScanPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
    }

    public ProjectScanResult scan(Path root) {
        Objects.requireNonNull(root, "root must not be null");
        List<Path> files = new ArrayList<>();
        List<String> relativeFiles = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        scanDirectory(root, root, 0, files, relativeFiles, warnings);
        return new ProjectScanResult(root, files, relativeFiles, warnings);
    }

    private void scanDirectory(Path root,
                               Path current,
                               int depth,
                               List<Path> files,
                               List<String> relativeFiles,
                               List<String> warnings) {
        if (depth > policy.maxDepth() || Files.isSymbolicLink(current)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
            for (Path child : stream) {
                String name = child.getFileName().toString();
                if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                    if (policy.isIgnoredDirectory(name)) {
                        continue;
                    }
                    scanDirectory(root, child, depth + 1, files, relativeFiles, warnings);
                    continue;
                }

                if (policy.isIgnoredFile(name)) {
                    continue;
                }

                files.add(child);
                relativeFiles.add(root.relativize(child).normalize().toString());
            }
        } catch (IOException exception) {
            warnings.add("Falha ao ler diretório: " + safeRelativeLabel(root, current));
        }
    }

    private String safeRelativeLabel(Path root, Path current) {
        try {
            return root.relativize(current).toString();
        } catch (Exception exception) {
            return current.getFileName() == null ? current.toString() : current.getFileName().toString();
        }
    }
}
