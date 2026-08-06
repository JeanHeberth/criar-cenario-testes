package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.learning.LearningScope;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Gera o id determinístico de um LearningItem via SHA-256 sobre uma
 * representação canônica (type + scope + título normalizado + componentes/
 * arquivos relacionados ordenados). Nunca usa UUID.randomUUID()/Math.random()
 * — mesma entrada sempre produz o mesmo id, independente da ordem original
 * das listas.
 */
public final class LearningIdGenerator {

    private LearningIdGenerator() {
    }

    public static String generate(LearningType type, LearningScope scope, String title,
                                   List<String> relatedComponents, List<String> relatedFiles) {
        String canonical = type + "|" + scope + "|" + normalizeTitle(title) + "|"
                + joinSorted(relatedComponents) + "|" + joinSorted(relatedFiles);
        return sha256Hex(canonical);
    }

    private static String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String joinSorted(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream().sorted().collect(Collectors.joining(","));
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
