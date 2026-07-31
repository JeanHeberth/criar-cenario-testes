package com.br.criarcenariotestes.business.autoqa.model.response;

import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.PackageManager;

import java.util.List;

/**
 * Resposta da validação rápida de caminho do projeto.
 * Não expõe informações desnecessárias sobre o sistema de arquivos.
 */
public record ProjectValidationResponse(

        boolean valid,

        String normalizedPath,

        boolean readable,

        boolean writable,

        AutomationFramework detectedFramework,

        AutomationLanguage detectedLanguage,

        PackageManager packageManager,

        String configurationFile,

        List<String> warnings

) {

    public static ProjectValidationResponse invalid(String warning) {
        return new ProjectValidationResponse(
                false, null, false, false, null, null, null, null, List.of(warning)
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean valid;
        private String normalizedPath;
        private boolean readable;
        private boolean writable;
        private AutomationFramework detectedFramework;
        private AutomationLanguage detectedLanguage;
        private PackageManager packageManager;
        private String configurationFile;
        private List<String> warnings = List.of();

        public Builder valid(boolean valid) { this.valid = valid; return this; }
        public Builder normalizedPath(String normalizedPath) { this.normalizedPath = normalizedPath; return this; }
        public Builder readable(boolean readable) { this.readable = readable; return this; }
        public Builder writable(boolean writable) { this.writable = writable; return this; }
        public Builder detectedFramework(AutomationFramework detectedFramework) { this.detectedFramework = detectedFramework; return this; }
        public Builder detectedLanguage(AutomationLanguage detectedLanguage) { this.detectedLanguage = detectedLanguage; return this; }
        public Builder packageManager(PackageManager packageManager) { this.packageManager = packageManager; return this; }
        public Builder configurationFile(String configurationFile) { this.configurationFile = configurationFile; return this; }
        public Builder warnings(List<String> warnings) { this.warnings = warnings != null ? warnings : List.of(); return this; }

        public ProjectValidationResponse build() {
            return new ProjectValidationResponse(valid, normalizedPath, readable, writable,
                    detectedFramework, detectedLanguage, packageManager, configurationFile, warnings);
        }
    }
}
