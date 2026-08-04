package com.br.criarcenariotestes.business.autoqa.discovery.parser;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Component
public class GradleBuildParser {

    private static final int MAX_TEXT_CHARS = 128_000;

    public void parse(Path file, String relativePath, ParsedProjectFiles.Builder builder) throws IOException {
        builder.gradleBuild(true);
        builder.gradleBuildPath(relativePath);
        builder.gradleContents().add(readLimitedText(file).toLowerCase(Locale.ROOT));
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
