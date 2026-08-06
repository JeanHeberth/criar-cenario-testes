package com.br.criarcenariotestes.business.autoqa.model.learning;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Enums de Learning - Testes Unitários")
class LearningEnumsTest {

    @Test
    @DisplayName("LearningType deve conter todos os tipos esperados")
    void learningTypeDeveConterTiposEsperados() {
        assertThat(LearningType.values()).containsExactly(
                LearningType.REUSABLE_COMPONENT,
                LearningType.NAMING_CONVENTION,
                LearningType.PROJECT_STRUCTURE,
                LearningType.TEST_PATTERN,
                LearningType.ASSERTION_PATTERN,
                LearningType.WAIT_STRATEGY,
                LearningType.DATA_STRATEGY,
                LearningType.ERROR_HANDLING,
                LearningType.COMMAND_PATTERN,
                LearningType.FRAMEWORK_RULE,
                LearningType.GENERATION_STRATEGY,
                LearningType.REVIEW_RULE,
                LearningType.FAILURE_PATTERN,
                LearningType.ANTI_PATTERN,
                LearningType.RISK_PATTERN,
                LearningType.RETRY_STRATEGY,
                LearningType.REGENERATION_STRATEGY,
                LearningType.UNKNOWN
        );
    }

    @Test
    @DisplayName("LearningScope deve conter EXECUTION, PROJECT e FRAMEWORK usáveis, além de TEAM e GLOBAL reservados")
    void learningScopeDeveConterEscoposEsperados() {
        assertThat(LearningScope.values()).containsExactly(
                LearningScope.EXECUTION,
                LearningScope.PROJECT,
                LearningScope.FRAMEWORK,
                LearningScope.TEAM,
                LearningScope.GLOBAL
        );
    }

    @Test
    @DisplayName("LearningSource deve conter todas as origens esperadas")
    void learningSourceDeveConterOrigensEsperadas() {
        assertThat(LearningSource.values()).containsExactly(
                LearningSource.DISCOVERY,
                LearningSource.SCENARIO_ANALYSIS,
                LearningSource.PROJECT_KNOWLEDGE,
                LearningSource.PLANNING,
                LearningSource.GENERATION,
                LearningSource.REVIEW,
                LearningSource.APPLY,
                LearningSource.EXECUTION,
                LearningSource.FAILURE_ANALYSIS,
                LearningSource.DETERMINISTIC_RULE,
                LearningSource.AI_SUGGESTION,
                LearningSource.HUMAN_APPROVAL,
                LearningSource.UNKNOWN
        );
    }

    @Test
    @DisplayName("LearningStatus deve conter todos os status esperados")
    void learningStatusDeveConterStatusEsperados() {
        assertThat(LearningStatus.values()).containsExactly(
                LearningStatus.COLLECTED,
                LearningStatus.COLLECTED_WITH_WARNINGS,
                LearningStatus.REVIEW_REQUIRED,
                LearningStatus.SKIPPED,
                LearningStatus.BLOCKED,
                LearningStatus.INVALID
        );
    }

    @Test
    @DisplayName("LearningConfidence deve conter todos os níveis esperados")
    void learningConfidenceDeveConterNiveisEsperados() {
        assertThat(LearningConfidence.values()).containsExactly(
                LearningConfidence.HIGH,
                LearningConfidence.MEDIUM,
                LearningConfidence.LOW,
                LearningConfidence.UNKNOWN
        );
    }

    @Test
    @DisplayName("LearningApprovalStatus deve conter todos os status esperados")
    void learningApprovalStatusDeveConterStatusEsperados() {
        assertThat(LearningApprovalStatus.values()).containsExactly(
                LearningApprovalStatus.PENDING,
                LearningApprovalStatus.APPROVED,
                LearningApprovalStatus.REJECTED,
                LearningApprovalStatus.NOT_REQUIRED
        );
    }
}
