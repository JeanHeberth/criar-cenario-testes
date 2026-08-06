package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyIoException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Utilitário de hashing SHA-256. Nunca usa timestamp como fonte de
 * integridade — apenas o conteúdo real dos bytes.
 */
@Component
public class ApplyHashValidator {

    private static final String ALGORITHM = "SHA-256";

    public String sha256(String content) {
        Objects.requireNonNull(content, "content must not be null");
        return sha256(content.getBytes(StandardCharsets.UTF_8));
    }

    public String sha256OfFile(Path file) {
        Objects.requireNonNull(file, "file must not be null");
        try {
            return sha256(Files.readAllBytes(file));
        } catch (IOException e) {
            throw new ApplyIoException("Falha ao ler arquivo para cálculo de hash: " + file, e);
        }
    }

    public boolean matches(String expected, String actual) {
        return expected != null && expected.equals(actual);
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(bytes);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível na JVM", e);
        }
    }
}
