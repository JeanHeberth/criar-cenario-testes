package com.br.criarcenariotestes.business.autoqa.model.context;

import com.br.criarcenariotestes.business.autoqa.model.enums.GeneratedFileOperation;

/**
 * Arquivo gerado pelo CodeGenerationAgent.
 * relativePath é sempre relativo à raiz do projeto — nunca absoluto, nunca com ../
 */
public record GeneratedFile(

        String relativePath,

        GeneratedFileOperation operation,

        String content,

        String explanation,

        String generatedHash

) {

    public boolean hasContent() {
        return content != null && !content.isBlank();
    }

    public boolean isRelativePath() {
        if (relativePath == null) return false;
        if (relativePath.startsWith("/")) return false;
        if (relativePath.length() >= 2 && Character.isLetter(relativePath.charAt(0))
                && relativePath.charAt(1) == ':') return false;
        return !relativePath.contains("..");
    }
}
