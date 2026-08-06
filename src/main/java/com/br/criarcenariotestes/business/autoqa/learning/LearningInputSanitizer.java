package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.learning.LearningEvidence;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningItem;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Redige segredos e limita a quantidade de evidências/itens antes de qualquer
 * envio ao provider de IA. Determinístico (ordena antes de truncar) e nunca
 * modifica os objetos originais.
 */
public class LearningInputSanitizer {

    private static final int MAX_EVIDENCE = 50;
    private static final int MAX_ITEMS = 50;

    private static final Pattern TOKEN = Pattern.compile("(?i)token=[^\\s,;]+");
    private static final Pattern PASSWORD = Pattern.compile("(?i)password=[^\\s,;]+");
    private static final Pattern API_KEY = Pattern.compile("(?i)api[_-]?key=[^\\s,;]+");
    private static final Pattern SECRET = Pattern.compile("(?i)secret=[^\\s,;]+");
    private static final Pattern URL_CREDENTIALS = Pattern.compile("(?i)://[^\\s:/@]+:[^\\s@/]+@");

    public List<LearningEvidence> sanitizeEvidence(List<LearningEvidence> evidence) {
        Objects.requireNonNull(evidence, "evidence must not be null");
        List<LearningEvidence> sorted = evidence.stream()
                .sorted(Comparator.comparing(LearningEvidence::source)
                        .thenComparing(LearningEvidence::referenceType)
                        .thenComparing(e -> e.referenceId() == null ? "" : e.referenceId()))
                .collect(Collectors.toList());
        List<LearningEvidence> limited = sorted.size() <= MAX_EVIDENCE ? sorted : sorted.subList(0, MAX_EVIDENCE);
        return limited.stream()
                .map(e -> new LearningEvidence(e.source(), e.referenceType(), e.referenceId(), e.relativePath(),
                        redact(e.description()), e.excerpt() == null ? null : redact(e.excerpt()), true))
                .collect(Collectors.collectingAndThen(Collectors.toList(), List::copyOf));
    }

    public List<LearningItem> sanitizeItems(List<LearningItem> items) {
        Objects.requireNonNull(items, "items must not be null");
        List<LearningItem> sorted = items.stream()
                .sorted(Comparator.comparing(LearningItem::id))
                .collect(Collectors.toList());
        List<LearningItem> limited = sorted.size() <= MAX_ITEMS ? sorted : sorted.subList(0, MAX_ITEMS);
        return limited.stream()
                .map(i -> new LearningItem(i.id(), i.type(), i.scope(), i.source(), redact(i.title()),
                        redact(i.description()), redact(i.recommendation()), i.confidence(), i.approvalStatus(),
                        i.evidence(), i.relatedComponents(), i.relatedFiles(), i.relatedFailureCodes(), i.tags(),
                        i.positive(), i.reusable(), i.humanReviewRequired()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), List::copyOf));
    }

    private String redact(String value) {
        if (value == null) {
            return null;
        }
        String redacted = TOKEN.matcher(value).replaceAll("[REDACTED]");
        redacted = PASSWORD.matcher(redacted).replaceAll("[REDACTED]");
        redacted = API_KEY.matcher(redacted).replaceAll("[REDACTED]");
        redacted = SECRET.matcher(redacted).replaceAll("[REDACTED]");
        redacted = URL_CREDENTIALS.matcher(redacted).replaceAll("://[REDACTED]@");
        return redacted;
    }
}
