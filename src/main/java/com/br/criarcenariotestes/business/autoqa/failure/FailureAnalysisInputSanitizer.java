package com.br.criarcenariotestes.business.autoqa.failure;

import com.br.criarcenariotestes.business.autoqa.model.failure.FailureEvidence;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FailureAnalysisInputSanitizer {

    public List<FailureEvidence> sanitizeEvidence(List<FailureEvidence> evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        // basic sanitization: limit size and mark sanitized
        return evidence.stream()
                .map(e -> new FailureEvidence(
                        e.source(),
                        e.relativePath(),
                        e.line(),
                        e.testName(),
                        redactSecrets(e.message()),
                        e.excerpt(),
                        true
                ))
                .limit(50)
                .collect(Collectors.toList());
    }

    private String redactSecrets(String s) {
        if (s == null) return null;
        // very simple redaction: remove tokens like "token=..." or "password=..."
        return s.replaceAll("(?i)token=[^\\s,;]+", "[REDACTED]")
                .replaceAll("(?i)password=[^\\s,;]+", "[REDACTED]")
                .replaceAll("(?i)api[_-]?key=[^\\s,;]+", "[REDACTED]");
    }
}