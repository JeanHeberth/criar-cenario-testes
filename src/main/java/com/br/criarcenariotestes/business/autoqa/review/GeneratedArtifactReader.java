package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.generation.GeneratedPathResolver;
import com.br.criarcenariotestes.business.autoqa.generation.GenerationHashService;
import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationWriteException;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.review.exception.CodeReviewReadException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lê somente os arquivos CREATE/UPDATE listados em GenerationResult, dentro de
 * .auto-qa/generated/&lt;executionId&gt;/files/. Nunca descobre arquivos via Files.walk
 * e nunca confia apenas em generatedRoot vindo do DTO — a raiz é sempre reconstruída
 * a partir de generatedBaseDir + executionId, resolvida com GeneratedPathResolver.
 *
 * Divergência de hash NÃO lança exceção aqui — é sinalizada em ReadArtifact.hashMatches()
 * para que o CodeReviewService decida (achado estático CRITICAL, sem chamar IA).
 */
@Component
public class GeneratedArtifactReader {

    static final int MAX_CONTENT_LENGTH = 20_000;
    static final int MAX_TOTAL_CONTENT_LENGTH = 100_000;

    private final GeneratedPathResolver pathResolver;
    private final GenerationHashService hashService;

    private Path generatedBaseDir = Path.of(".auto-qa", "generated");

    public GeneratedArtifactReader(GeneratedPathResolver pathResolver, GenerationHashService hashService) {
        this.pathResolver = Objects.requireNonNull(pathResolver, "pathResolver must not be null");
        this.hashService = Objects.requireNonNull(hashService, "hashService must not be null");
    }

    /** Visível apenas para testes: permite isolar a leitura em um diretório temporário. */
    void setGeneratedBaseDir(Path generatedBaseDir) {
        this.generatedBaseDir = Objects.requireNonNull(generatedBaseDir, "generatedBaseDir must not be null");
    }

    public List<ReadArtifact> readAll(UUID executionId, GenerationResult generation) {
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(generation, "generation must not be null");

        List<ReadArtifact> results = new ArrayList<>();
        long totalLength = 0;

        for (GeneratedFile file : generation.files()) {
            if (file == null) continue;
            if (file.operation() != GeneratedFileOperation.CREATE && file.operation() != GeneratedFileOperation.UPDATE) {
                continue;
            }
            ReadArtifact artifact = readOne(executionId, file);
            totalLength += artifact.content().length();
            if (totalLength > MAX_TOTAL_CONTENT_LENGTH) {
                throw new CodeReviewReadException("Conteúdo total dos arquivos gerados excede o limite permitido");
            }
            results.add(artifact);
        }
        return List.copyOf(results);
    }

    private ReadArtifact readOne(UUID executionId, GeneratedFile file) {
        Path target;
        try {
            target = pathResolver.resolve(generatedBaseDir, executionId, file.relativePath());
        } catch (GenerationWriteException e) {
            throw new CodeReviewReadException("Caminho inseguro para arquivo gerado: " + file.relativePath(), e);
        }

        if (Files.isSymbolicLink(target)) {
            throw new CodeReviewReadException("Symlink não permitido: " + file.relativePath());
        }
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new CodeReviewReadException("Arquivo gerado não encontrado: " + file.relativePath());
        }

        String content;
        try {
            content = Files.readString(target, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CodeReviewReadException("Falha de I/O ao ler arquivo gerado: " + file.relativePath(), e);
        }

        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new CodeReviewReadException("Arquivo gerado excede o limite de tamanho: " + file.relativePath());
        }

        String actualSha256 = hashService.sha256(content).hex();
        boolean hashMatches = actualSha256.equals(file.sha256());

        return new ReadArtifact(file.relativePath(), file.operation(), file.componentType(), content, actualSha256, hashMatches);
    }

    public record ReadArtifact(
            String relativePath,
            GeneratedFileOperation operation,
            PlanComponentType componentType,
            String content,
            String actualSha256,
            boolean hashMatches
    ) {}
}
