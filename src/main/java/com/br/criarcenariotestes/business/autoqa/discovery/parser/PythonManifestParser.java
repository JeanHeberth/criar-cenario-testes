package com.br.criarcenariotestes.business.autoqa.discovery.parser;

import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Component
public class PythonManifestParser {

    private static final int MAX_TEXT_CHARS = 128_000;

    public void parseRequirements(Path file, String relativePath, ParsedProjectFiles.Builder builder) throws IOException {
        builder.requirementsTxt(true);
        builder.requirementsPath(relativePath);
        builder.requirementsContents().add(readLimitedText(file).toLowerCase(Locale.ROOT));
        builder.packageManagerCandidates().add(PackageManager.PIP);
    }

    public void parsePyproject(Path file, String relativePath, ParsedProjectFiles.Builder builder) throws IOException {
        builder.pyprojectToml(true);
        builder.pyprojectPath(relativePath);
        String content = readLimitedText(file).toLowerCase(Locale.ROOT);
        builder.pyprojectContents().add(content);
        if (content.contains("[tool.poetry]")) {
            builder.packageManagerCandidates().add(PackageManager.POETRY);
        }
    }

    public void parsePoetryLock(Path file, String relativePath, ParsedProjectFiles.Builder builder) throws IOException {
        builder.poetryLock(true);
        builder.poetryLockPath(relativePath);
        builder.poetryContents().add(readLimitedText(file).toLowerCase(Locale.ROOT));
        builder.packageManagerCandidates().add(PackageManager.POETRY);
    }

    private String readLimitedText(Path file) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            char[] buffer = new char[4096];
            int total = 0;
            int read;
            while ((read = reader.read(buffer)) != -1 && total < MAX_TEXT_CHARS) {
                int toAppend = Math.min(read, MAX_TEXT_CHARS - total);
                builder.append(buffer, 0, toAppend);
                total += toAppend;
            }
        }
        return builder.toString();
    }
}
