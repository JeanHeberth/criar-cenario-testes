package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.learning.LearningScope;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LearningIdGenerator - Testes Unitários")
class LearningIdGeneratorTest {

    @Test
    @DisplayName("Mesma entrada deve gerar o mesmo ID")
    void mesmaEntradaGeraMesmoId() {
        String id1 = LearningIdGenerator.generate(LearningType.REUSABLE_COMPONENT, LearningScope.PROJECT,
                "Componente reutilizado", List.of("PageObject"), List.of("tests/login.spec.ts"));
        String id2 = LearningIdGenerator.generate(LearningType.REUSABLE_COMPONENT, LearningScope.PROJECT,
                "Componente reutilizado", List.of("PageObject"), List.of("tests/login.spec.ts"));
        assertThat(id1).isEqualTo(id2);
    }

    @Test
    @DisplayName("Ordem original das listas não deve alterar o ID")
    void ordemDasListasNaoAlteraId() {
        String id1 = LearningIdGenerator.generate(LearningType.REUSABLE_COMPONENT, LearningScope.PROJECT,
                "Componente reutilizado", List.of("A", "B"), List.of("x.ts", "y.ts"));
        String id2 = LearningIdGenerator.generate(LearningType.REUSABLE_COMPONENT, LearningScope.PROJECT,
                "Componente reutilizado", List.of("B", "A"), List.of("y.ts", "x.ts"));
        assertThat(id1).isEqualTo(id2);
    }

    @Test
    @DisplayName("Título com capitalização/espaçamento diferente deve gerar o mesmo ID")
    void tituloNormalizadoGeraMesmoId() {
        String id1 = LearningIdGenerator.generate(LearningType.REUSABLE_COMPONENT, LearningScope.PROJECT,
                "Componente Reutilizado", List.of(), List.of());
        String id2 = LearningIdGenerator.generate(LearningType.REUSABLE_COMPONENT, LearningScope.PROJECT,
                "  componente   reutilizado  ", List.of(), List.of());
        assertThat(id1).isEqualTo(id2);
    }

    @Test
    @DisplayName("Type diferente deve gerar ID diferente")
    void typeDiferenteGeraIdDiferente() {
        String id1 = LearningIdGenerator.generate(LearningType.REUSABLE_COMPONENT, LearningScope.PROJECT, "t", List.of(), List.of());
        String id2 = LearningIdGenerator.generate(LearningType.ANTI_PATTERN, LearningScope.PROJECT, "t", List.of(), List.of());
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("Scope diferente deve gerar ID diferente")
    void scopeDiferenteGeraIdDiferente() {
        String id1 = LearningIdGenerator.generate(LearningType.REUSABLE_COMPONENT, LearningScope.PROJECT, "t", List.of(), List.of());
        String id2 = LearningIdGenerator.generate(LearningType.REUSABLE_COMPONENT, LearningScope.EXECUTION, "t", List.of(), List.of());
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("relatedComponents diferentes devem gerar ID diferente")
    void relatedComponentsDiferentesGeramIdDiferente() {
        String id1 = LearningIdGenerator.generate(LearningType.REUSABLE_COMPONENT, LearningScope.PROJECT, "t", List.of("A"), List.of());
        String id2 = LearningIdGenerator.generate(LearningType.REUSABLE_COMPONENT, LearningScope.PROJECT, "t", List.of("B"), List.of());
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("ID deve ser um hex SHA-256 de 64 caracteres, não um hashCode simples")
    void idDeveSerHexSha256() {
        String id = LearningIdGenerator.generate(LearningType.UNKNOWN, LearningScope.EXECUTION, "t", List.of(), List.of());
        assertThat(id).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("Deve tratar listas nulas como vazias sem lançar exceção")
    void deveTratarListasNulas() {
        String id = LearningIdGenerator.generate(LearningType.UNKNOWN, LearningScope.EXECUTION, "t", null, null);
        assertThat(id).matches("[0-9a-f]{64}");
    }
}
