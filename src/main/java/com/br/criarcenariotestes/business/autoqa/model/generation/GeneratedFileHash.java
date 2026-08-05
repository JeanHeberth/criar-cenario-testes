package com.br.criarcenariotestes.business.autoqa.model.generation;

public record GeneratedFileHash(
        String algorithm,
        String hex
) {
    public GeneratedFileHash {
        algorithm = algorithm == null ? null : algorithm.trim();
        hex = hex == null ? null : hex.trim();
    }
}
