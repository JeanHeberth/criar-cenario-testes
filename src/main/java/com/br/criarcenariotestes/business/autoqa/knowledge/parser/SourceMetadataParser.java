package com.br.criarcenariotestes.business.autoqa.knowledge.parser;

import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface SourceMetadataParser {

    boolean supports(KnowledgeScanResult.KnowledgeFile file);

    SourceMetadata parse(KnowledgeScanResult.KnowledgeFile file);

    record SourceMetadata(
            String relativePath,
            String name,
            SourceLanguage language,
            String packageName,
            List<String> declaredClasses,
            List<String> declaredMethods,
            List<String> imports,
            List<String> annotations,
            List<String> hierarchy,
            List<String> tags,
            boolean testComponent,
            List<String> warnings
    ) {
        public SourceMetadata {
            declaredClasses = declaredClasses == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(declaredClasses));
            declaredMethods = declaredMethods == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(declaredMethods));
            imports = imports == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(imports));
            annotations = annotations == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(annotations));
            hierarchy = hierarchy == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(hierarchy));
            tags = tags == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(tags));
            warnings = warnings == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(warnings));
        }
    }
}
