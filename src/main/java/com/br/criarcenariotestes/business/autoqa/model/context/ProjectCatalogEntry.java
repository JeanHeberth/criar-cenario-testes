package com.br.criarcenariotestes.business.autoqa.model.context;

import java.util.Locale;

/**
 * Entrada individual do catálogo de arquivos do projeto de automação.
 * O conteúdo é carregado apenas para arquivos de texto dentro dos limites.
 */
public record ProjectCatalogEntry(

        String relativePath,

        long sizeBytes,

        boolean contentLoaded,

        String content,

        boolean prioritized

) {

    public String fileExtension() {
        int dot = relativePath.lastIndexOf('.');
        if (dot < 0) return "";
        return relativePath.substring(dot).toLowerCase(Locale.ROOT);
    }

    public String fileName() {
        int slash = Math.max(relativePath.lastIndexOf('/'), relativePath.lastIndexOf('\\'));
        return slash >= 0 ? relativePath.substring(slash + 1) : relativePath;
    }

    public boolean isTypeScript() {
        return ".ts".equals(fileExtension()) || ".mts".equals(fileExtension());
    }

    public boolean isJavaScript() {
        return ".js".equals(fileExtension()) || ".mjs".equals(fileExtension());
    }

    public boolean isJson() {
        return ".json".equals(fileExtension());
    }

    public boolean hasContent() {
        return contentLoaded && content != null && !content.isBlank();
    }

    public long sizeKb() {
        return sizeBytes / 1024;
    }
}
