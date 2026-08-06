package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.learning.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LearningInputSanitizer - Testes Unitários")
class LearningInputSanitizerTest {

    private final LearningInputSanitizer sanitizer = new LearningInputSanitizer();

    @Test
    @DisplayName("Deve redigir token na descrição da evidência")
    void deveRedigirTokenNaDescricao() {
        LearningEvidence ev = new LearningEvidence("execution", "test-summary", "id", null,
                "falha com token=abc123secreto no log", null, false);
        List<LearningEvidence> sanitized = sanitizer.sanitizeEvidence(List.of(ev));
        assertThat(sanitized.get(0).description()).doesNotContain("abc123secreto").contains("[REDACTED]");
    }

    @Test
    @DisplayName("Deve redigir password na descrição da evidência")
    void deveRedigirPasswordNaDescricao() {
        LearningEvidence ev = new LearningEvidence("execution", "test-summary", "id", null,
                "login com password=minhasenha123", null, false);
        List<LearningEvidence> sanitized = sanitizer.sanitizeEvidence(List.of(ev));
        assertThat(sanitized.get(0).description()).doesNotContain("minhasenha123");
    }

    @Test
    @DisplayName("Deve redigir apiKey na descrição da evidência")
    void deveRedigirApiKeyNaDescricao() {
        LearningEvidence ev = new LearningEvidence("execution", "test-summary", "id", null,
                "chamada com api_key=xyz789", null, false);
        List<LearningEvidence> sanitized = sanitizer.sanitizeEvidence(List.of(ev));
        assertThat(sanitized.get(0).description()).doesNotContain("xyz789");
    }

    @Test
    @DisplayName("Deve redigir credenciais em URL")
    void deveRedigirCredenciaisEmUrl() {
        LearningEvidence ev = new LearningEvidence("execution", "test-summary", "id", null,
                "conexão via https://user:supersecret@host.com/api", null, false);
        List<LearningEvidence> sanitized = sanitizer.sanitizeEvidence(List.of(ev));
        assertThat(sanitized.get(0).description()).doesNotContain("supersecret");
    }

    @Test
    @DisplayName("Deve marcar evidência sanitizada como sanitized=true")
    void deveMarcarEvidenciaComoSanitizada() {
        LearningEvidence ev = new LearningEvidence("execution", "test-summary", "id", null, "descrição limpa", null, false);
        List<LearningEvidence> sanitized = sanitizer.sanitizeEvidence(List.of(ev));
        assertThat(sanitized.get(0).sanitized()).isTrue();
    }

    @Test
    @DisplayName("Deve limitar a quantidade de evidências")
    void deveLimitarQuantidadeDeEvidencias() {
        List<LearningEvidence> many = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            many.add(new LearningEvidence("execution", "test-summary", "id" + i, null, "descrição " + i, null, false));
        }
        List<LearningEvidence> sanitized = sanitizer.sanitizeEvidence(many);
        assertThat(sanitized.size()).isLessThanOrEqualTo(50);
    }

    @Test
    @DisplayName("Deve ser determinístico ao ordenar antes de truncar")
    void deveSerDeterministico() {
        List<LearningEvidence> many = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            many.add(new LearningEvidence("execution", "test-summary", "id" + i, null, "descrição " + i, null, false));
        }
        List<LearningEvidence> sanitized1 = sanitizer.sanitizeEvidence(many);
        List<LearningEvidence> sanitized2 = sanitizer.sanitizeEvidence(many);
        assertThat(sanitized1).isEqualTo(sanitized2);
    }

    @Test
    @DisplayName("Não deve modificar a lista original de evidências")
    void naoDeveModificarListaOriginalDeEvidencias() {
        LearningEvidence ev = new LearningEvidence("execution", "test-summary", "id", null, "token=abc123", null, false);
        List<LearningEvidence> original = List.of(ev);
        sanitizer.sanitizeEvidence(original);
        assertThat(original.get(0).description()).isEqualTo("token=abc123");
    }

    @Test
    @DisplayName("Deve rejeitar lista de evidências nula")
    void deveRejeitarListaDeEvidenciasNula() {
        assertThatThrownBy(() -> sanitizer.sanitizeEvidence(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve redigir segredo no título e descrição do item")
    void deveRedigirSegredoNoItem() {
        LearningItem item = itemComTitulo("Comando com token=abc123secreto");
        List<LearningItem> sanitized = sanitizer.sanitizeItems(List.of(item));
        assertThat(sanitized.get(0).title()).doesNotContain("abc123secreto");
    }

    @Test
    @DisplayName("Deve limitar a quantidade de itens")
    void deveLimitarQuantidadeDeItens() {
        List<LearningItem> many = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            many.add(itemComTituloEId("id" + i, "título " + i));
        }
        List<LearningItem> sanitized = sanitizer.sanitizeItems(many);
        assertThat(sanitized.size()).isLessThanOrEqualTo(50);
    }

    @Test
    @DisplayName("Não deve modificar a lista original de itens")
    void naoDeveModificarListaOriginalDeItens() {
        LearningItem item = itemComTitulo("Comando com token=abc123secreto");
        List<LearningItem> original = List.of(item);
        sanitizer.sanitizeItems(original);
        assertThat(original.get(0).title()).contains("abc123secreto");
    }

    @Test
    @DisplayName("Deve rejeitar lista de itens nula")
    void deveRejeitarListaDeItensNula() {
        assertThatThrownBy(() -> sanitizer.sanitizeItems(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve retornar lista imutável de evidências")
    void deveRetornarListaImutavelDeEvidencias() {
        List<LearningEvidence> sanitized = sanitizer.sanitizeEvidence(List.of());
        assertThatThrownBy(() -> sanitized.add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve retornar lista imutável de itens")
    void deveRetornarListaImutavelDeItens() {
        List<LearningItem> sanitized = sanitizer.sanitizeItems(List.of());
        assertThatThrownBy(() -> sanitized.add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    private LearningItem itemComTitulo(String title) {
        return itemComTituloEId("id", title);
    }

    private LearningItem itemComTituloEId(String id, String title) {
        return new LearningItem(id, LearningType.COMMAND_PATTERN, LearningScope.EXECUTION, LearningSource.EXECUTION,
                title, "descrição", "recomendação", LearningConfidence.MEDIUM, LearningApprovalStatus.NOT_REQUIRED,
                List.of(), List.of(), List.of(), List.of(), List.of(), true, true, false);
    }
}
