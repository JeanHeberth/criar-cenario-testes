package com.br.criarcenariotestes.business.autoqa.model.enums;

public enum PackageManager {

    NPM("npm", "package-lock.json"),
    YARN("yarn", "yarn.lock"),
    PNPM("pnpm", "pnpm-lock.yaml"),
    UNKNOWN("desconhecido", null);

    private final String descricao;
    private final String lockFile;

    PackageManager(String descricao, String lockFile) {
        this.descricao = descricao;
        this.lockFile = lockFile;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getLockFile() {
        return lockFile;
    }
}
