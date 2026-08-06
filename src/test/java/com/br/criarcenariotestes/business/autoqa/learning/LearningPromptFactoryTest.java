package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.learning.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LearningPromptFactory - Testes Unitários")
class LearningPromptFactoryTest {

    private final LearningPromptFactory factory = new LearningPromptFactory();

    @Test
    @DisplayName("System prompt deve exigir somente JSON")
    void systemPromptDeveExigirSomenteJson() {
        assertThat(factory.systemPrompt()).containsIgnoringCase("JSON");
    }

    @Test
    @DisplayName("System prompt deve proibir Markdown")
    void systemPromptDeveProibirMarkdown() {
        assertThat(factory.systemPrompt()).containsIgnoringCase("markdown");
    }

    @Test
    @DisplayName("System prompt deve proibir código, diff, patch e comando")
    void systemPromptDeveProibirCodigoDiffPatchComando() {
        String prompt = factory.systemPrompt().toLowerCase();
        assertThat(prompt).contains("código").contains("diff").contains("patch").contains("comando");
    }

    @Test
    @DisplayName("System prompt deve proibir caminho absoluto e segredo")
    void systemPromptDeveProibirCaminhoAbsolutoESegredo() {
        String prompt = factory.systemPrompt().toLowerCase();
        assertThat(prompt).contains("absoluto").contains("segredo");
    }

    @Test
    @DisplayName("System prompt deve proibir invenção de evidência/componente/arquivo")
    void systemPromptDeveProibirInvencao() {
        String prompt = factory.systemPrompt().toLowerCase();
        assertThat(prompt).contains("não invente").contains("evidência");
    }

    @Test
    @DisplayName("System prompt deve proibir promoção a GLOBAL e aprovação automática")
    void systemPromptDeveProibirGlobalEAprovacaoAutomatica() {
        String prompt = factory.systemPrompt().toLowerCase();
        assertThat(prompt).contains("global").contains("aprov");
    }

    @Test
    @DisplayName("System prompt deve proibir remoção de aprendizado determinístico")
    void systemPromptDeveProibirRemocaoDeterministico() {
        assertThat(factory.systemPrompt().toLowerCase()).contains("determinístic");
    }

    @Test
    @DisplayName("System prompt deve exigir português do Brasil")
    void systemPromptDeveExigirPortugues() {
        assertThat(factory.systemPrompt().toLowerCase()).contains("português");
    }

    @Test
    @DisplayName("User prompt deve conter o executionId")
    void userPromptDeveConterExecutionId() {
        UUID executionId = UUID.randomUUID();
        String prompt = factory.userPrompt(executionId, List.of(), List.of(), List.of());
        assertThat(prompt).contains(executionId.toString());
    }

    @Test
    @DisplayName("User prompt deve conter descrição da evidência")
    void userPromptDeveConterEvidencia() {
        LearningEvidence ev = new LearningEvidence("execution", "test-summary", "id", null, "descrição da evidência", null, true);
        String prompt = factory.userPrompt(UUID.randomUUID(), List.of(), List.of(ev), List.of());
        assertThat(prompt).contains("descrição da evidência");
    }

    @Test
    @DisplayName("User prompt não deve conter caminho absoluto")
    void userPromptNaoDeveConterCaminhoAbsoluto() {
        String prompt = factory.userPrompt(UUID.randomUUID(), List.of(), List.of(), List.of());
        assertThat(prompt).doesNotContain("/Users/").doesNotContain("/home/");
    }

    @Test
    @DisplayName("User prompt deve ser JSON válido (estruturalmente balanceado)")
    void userPromptDeveSerJsonValido() {
        String prompt = factory.userPrompt(UUID.randomUUID(), List.of(), List.of(), List.of());
        long open = prompt.chars().filter(c -> c == '{').count();
        long close = prompt.chars().filter(c -> c == '}').count();
        assertThat(open).isEqualTo(close);
    }
}
