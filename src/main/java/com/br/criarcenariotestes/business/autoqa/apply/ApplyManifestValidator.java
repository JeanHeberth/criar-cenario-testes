package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyConflict;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationManifest;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lê manifest.json de .auto-qa/generated/&lt;executionId&gt;/manifest.json e
 * compara com GenerationResult. Nunca lança exceção: qualquer problema (arquivo
 * ausente, JSON corrompido, divergência de conteúdo) vira um ApplyConflict do
 * tipo MANIFEST_MISMATCH, deixando a decisão (BLOCKED) para FileApplicationService.
 */
@Component
public class ApplyManifestValidator {

    private final ObjectMapper objectMapper;

    private Path generatedBaseDir = Path.of(".auto-qa", "generated");

    public ApplyManifestValidator(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null").copy();
    }

    /** Visível apenas para testes: permite isolar a leitura em um diretório temporário. */
    void setGeneratedBaseDir(Path generatedBaseDir) {
        this.generatedBaseDir = Objects.requireNonNull(generatedBaseDir, "generatedBaseDir must not be null");
    }

    public List<ApplyConflict> validate(UUID executionId, ProjectDiscoveryResult discovery, GenerationResult generation) {
        Objects.requireNonNull(executionId, "executionId must not be null");
        Objects.requireNonNull(discovery, "discovery must not be null");
        Objects.requireNonNull(generation, "generation must not be null");

        Path manifestPath = generatedBaseDir.resolve(executionId.toString()).resolve("manifest.json").normalize();

        GenerationManifest manifest;
        try {
            if (!Files.exists(manifestPath) || !Files.isRegularFile(manifestPath)) {
                return List.of(new ApplyConflict(null, ApplyConflict.MANIFEST_MISMATCH,
                        "Manifest não encontrado: " + manifestPath));
            }
            String json = Files.readString(manifestPath, StandardCharsets.UTF_8);
            manifest = objectMapper.readValue(json, GenerationManifest.class);
        } catch (IOException e) {
            return List.of(new ApplyConflict(null, ApplyConflict.MANIFEST_MISMATCH,
                    "Falha ao ler manifest: " + e.getMessage()));
        }

        List<ApplyConflict> conflicts = new ArrayList<>();

        if (!executionId.equals(manifest.executionId())) {
            conflicts.add(new ApplyConflict(null, ApplyConflict.MANIFEST_MISMATCH, "executionId divergente no manifest"));
        }
        if (!safeEquals(manifest.framework(), discovery.getAutomationFramework().name())) {
            conflicts.add(new ApplyConflict(null, ApplyConflict.MANIFEST_MISMATCH, "framework divergente no manifest"));
        }
        if (!safeEquals(manifest.language(), discovery.getLanguage().name())) {
            conflicts.add(new ApplyConflict(null, ApplyConflict.MANIFEST_MISMATCH, "linguagem divergente no manifest"));
        }

        Map<String, GenerationManifest.GenerationManifestFile> manifestFiles = manifest.files().stream()
                .filter(f -> f != null && f.relativePath() != null)
                .collect(Collectors.toMap(GenerationManifest.GenerationManifestFile::relativePath, f -> f, (a, b) -> a));
        Map<String, GeneratedFile> generatedFiles = generation.files().stream()
                .filter(f -> f != null && f.relativePath() != null)
                .collect(Collectors.toMap(GeneratedFile::relativePath, f -> f, (a, b) -> a));

        for (String relativePath : manifestFiles.keySet()) {
            if (!generatedFiles.containsKey(relativePath)) {
                conflicts.add(new ApplyConflict(relativePath, ApplyConflict.MANIFEST_MISMATCH,
                        "Arquivo presente no manifest mas ausente na geração: " + relativePath));
            }
        }
        for (String relativePath : generatedFiles.keySet()) {
            if (!manifestFiles.containsKey(relativePath)) {
                conflicts.add(new ApplyConflict(relativePath, ApplyConflict.MANIFEST_MISMATCH,
                        "Arquivo presente na geração mas omitido do manifest: " + relativePath));
            }
        }
        for (String relativePath : generatedFiles.keySet()) {
            GenerationManifest.GenerationManifestFile manifestFile = manifestFiles.get(relativePath);
            if (manifestFile == null) continue;
            GeneratedFile generatedFile = generatedFiles.get(relativePath);
            if (!safeEquals(manifestFile.operation(), generatedFile.operation().name())) {
                conflicts.add(new ApplyConflict(relativePath, ApplyConflict.MANIFEST_MISMATCH,
                        "Operação divergente no manifest para " + relativePath));
            }
            if (generatedFile.sha256() != null && !safeEquals(manifestFile.sha256(), generatedFile.sha256())) {
                conflicts.add(new ApplyConflict(relativePath, ApplyConflict.MANIFEST_MISMATCH,
                        "Hash divergente no manifest para " + relativePath));
            }
        }

        return List.copyOf(conflicts);
    }

    private boolean safeEquals(String a, String b) {
        return a != null && a.equals(b);
    }
}
