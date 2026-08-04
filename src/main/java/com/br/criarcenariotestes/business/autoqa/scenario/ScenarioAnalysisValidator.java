package com.br.criarcenariotestes.business.autoqa.scenario;

import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
import com.br.criarcenariotestes.business.autoqa.model.scenario.BusinessRule;
import com.br.criarcenariotestes.business.autoqa.model.scenario.RiskLevel;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisStatus;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAmbiguity;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioRisk;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioStep;
import com.br.criarcenariotestes.business.autoqa.model.scenario.TestDataRequirement;
import com.br.criarcenariotestes.business.autoqa.model.scenario.TestDataType;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ScenarioAnalysisValidator {

    private static final Pattern UNIX_ABSOLUTE_PATH = Pattern.compile("(?<!\\S)/(?:[^\\s/]+/)*[^\\s/]+");
    private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile("(?i)(?<!\\S)[A-Z]:[\\\\/](?:[^\\s\\\\/]+[\\\\/])*[^\\s\\\\/]+");
    private static final Pattern UNC_ABSOLUTE_PATH = Pattern.compile("(?<!\\S)\\\\\\\\[^\\s\\\\/]+\\\\[^\\s\\\\/]+(?:\\\\[^\\s\\\\/]+)*");
    private static final Pattern FILE_URI = Pattern.compile("(?i)(?<!\\S)file://\\S+");
    private static final Pattern SECRET_PATTERN = Pattern.compile("(?i)(password|senha|token|api[_-]?key|apikey|secret)\\s*[:=]\\s*\\S+");
    private static final Pattern CODE_PATTERN = Pattern.compile("(?i)\\b(class|import|public|private|protected|return|function|def|let|const|var|new)\\b|[;{}]|=>");
    private static final Pattern COMMAND_PATTERN = Pattern.compile("(?i)\\b(curl|npm|yarn|pnpm|mvn|gradle|git|docker|kubectl|chmod|ssh)\\b");

    public ScenarioAnalysisResult validate(ScenarioAnalysisResult result) {
        Objects.requireNonNull(result, "result must not be null");

        validateText(result.title(), "title");
        validateText(result.objective(), "objective");
        validateObjectList(result.steps(), "steps");
        if (result.steps().isEmpty()) {
            throw new ScenarioAnalysisValidationException("Lista de passos vazia");
        }

        validateSteps(result.steps());
        validateTestData(result.testData());
        validateTextFields(result);
        validateStatus(result);
        return result;
    }

    private void validateSteps(List<ScenarioStep> steps) {
        Set<Integer> seenOrders = new HashSet<>();
        int expectedOrder = 1;
        for (int index = 0; index < steps.size(); index++) {
            ScenarioStep step = steps.get(index);
            if (step == null) {
                throw new ScenarioAnalysisValidationException("steps[" + index + "] nulo");
            }
            if (step.order() <= 0) {
                throw new ScenarioAnalysisValidationException("steps[" + index + "].order inválido");
            }
            if (!seenOrders.add(step.order())) {
                throw new ScenarioAnalysisValidationException("steps[" + index + "].order duplicado");
            }
            if (step.order() != expectedOrder) {
                throw new ScenarioAnalysisValidationException("steps[" + index + "] fora de ordem");
            }
            validateText(step.action(), "steps[" + index + "].action");
            validateText(step.expectedResult(), "steps[" + index + "].expectedResult");
            validateTextList(step.dependencies(), "steps[" + index + "].dependencies");
            ensureNoForbiddenContent(step.action());
            ensureNoForbiddenContent(step.expectedResult());
            expectedOrder++;
        }
    }

    private void validateTestData(List<TestDataRequirement> testData) {
        validateObjectList(testData, "testData");
        for (int index = 0; index < testData.size(); index++) {
            TestDataRequirement requirement = testData.get(index);
            if (requirement == null) {
                throw new ScenarioAnalysisValidationException("testData[" + index + "] nulo");
            }
            validateText(requirement.name(), "testData[" + index + "].name");
            validateText(requirement.description(), "testData[" + index + "].description");
            validateEnum(requirement.type(), "testData[" + index + "].type");
            if (requirement.type() == TestDataType.SECRET && requirement.example() != null) {
                throw new ScenarioAnalysisValidationException("testData[" + index + "].example não pode expor segredo");
            }
            ensureNoForbiddenContent(requirement.name());
            ensureNoForbiddenContent(requirement.description());
            if (requirement.example() != null) {
                ensureNoForbiddenContent(requirement.example());
            }
        }
    }

    private void validateTextFields(ScenarioAnalysisResult result) {
        ensureNoForbiddenContent(result.title());
        ensureNoForbiddenContent(result.objective());
        validateTextList(result.preconditions(), "preconditions");
        validateTextList(result.entities(), "entities");
        validateTextList(result.dependencies(), "dependencies");
        validateTextList(result.warnings(), "warnings");
        validateAmbiguities(result.ambiguities());
        validateBusinessRules(result.businessRules());
        validateRisks(result.risks());
    }

    private void validateStatus(ScenarioAnalysisResult result) {
        validateEnum(result.automationType(), "automationType");
        validateEnum(result.status(), "status");

        boolean hasBlockingAmbiguity = result.ambiguities().stream().anyMatch(ScenarioAmbiguity::blocking);
        boolean hasWarnings = !result.warnings().isEmpty() || !result.ambiguities().isEmpty();

        if (hasBlockingAmbiguity) {
            if (result.status() != ScenarioAnalysisStatus.INVALID || result.valid()) {
                throw new ScenarioAnalysisValidationException("Status incoerente para ambiguidades bloqueantes");
            }
            return;
        }

        if (result.status() == ScenarioAnalysisStatus.INVALID) {
            throw new ScenarioAnalysisValidationException("Status INVALID sem ambiguidade bloqueante");
        }
        if (hasWarnings && result.status() != ScenarioAnalysisStatus.VALID_WITH_WARNINGS) {
            throw new ScenarioAnalysisValidationException("Status incoerente com warnings");
        }
        if (!hasWarnings && result.status() != ScenarioAnalysisStatus.VALID) {
            throw new ScenarioAnalysisValidationException("Status incoerente com resultado limpo");
        }
        if (!result.valid()) {
            throw new ScenarioAnalysisValidationException("Campo valid incoerente");
        }
    }

    private void validateBusinessRules(List<BusinessRule> businessRules) {
        validateObjectList(businessRules, "businessRules");
        for (int index = 0; index < businessRules.size(); index++) {
            BusinessRule businessRule = businessRules.get(index);
            if (businessRule == null) {
                throw new ScenarioAnalysisValidationException("businessRules[" + index + "] nulo");
            }
            validateText(businessRule.identifier(), "businessRules[" + index + "].identifier");
            validateText(businessRule.description(), "businessRules[" + index + "].description");
            ensureNoForbiddenContent(businessRule.identifier());
            ensureNoForbiddenContent(businessRule.description());
        }
    }

    private void validateRisks(List<ScenarioRisk> risks) {
        validateObjectList(risks, "risks");
        for (int index = 0; index < risks.size(); index++) {
            ScenarioRisk risk = risks.get(index);
            if (risk == null) {
                throw new ScenarioAnalysisValidationException("risks[" + index + "] nulo");
            }
            validateText(risk.description(), "risks[" + index + "].description");
            validateText(risk.mitigation(), "risks[" + index + "].mitigation");
            validateEnum(risk.level(), "risks[" + index + "].level");
            ensureNoForbiddenContent(risk.description());
            ensureNoForbiddenContent(risk.mitigation());
        }
    }

    private void validateAmbiguities(List<ScenarioAmbiguity> ambiguities) {
        validateObjectList(ambiguities, "ambiguities");
        for (int index = 0; index < ambiguities.size(); index++) {
            ScenarioAmbiguity ambiguity = ambiguities.get(index);
            if (ambiguity == null) {
                throw new ScenarioAnalysisValidationException("ambiguities[" + index + "] nulo");
            }
            validateText(ambiguity.description(), "ambiguities[" + index + "].description");
            validateText(ambiguity.question(), "ambiguities[" + index + "].question");
            ensureNoForbiddenContent(ambiguity.description());
            ensureNoForbiddenContent(ambiguity.question());
        }
    }

    private void validateTextList(List<String> values, String fieldName) {
        validateObjectList(values, fieldName);
        for (int index = 0; index < values.size(); index++) {
            String value = values.get(index);
            if (value == null) {
                throw new ScenarioAnalysisValidationException(fieldName + "[" + index + "] nulo");
            }
            validateText(value, fieldName + "[" + index + "]");
            ensureNoForbiddenContent(value);
        }
    }

    private void validateObjectList(List<?> values, String fieldName) {
        if (values == null) {
            throw new ScenarioAnalysisValidationException(fieldName + " ausente");
        }
    }

    private void validateText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ScenarioAnalysisValidationException(fieldName + " ausente");
        }
    }

    private void validateEnum(Enum<?> value, String fieldName) {
        if (value == null) {
            throw new ScenarioAnalysisValidationException(fieldName + " ausente");
        }
        if ("UNKNOWN".equals(value.name())) {
            throw new ScenarioAnalysisValidationException(fieldName + " inválido");
        }
    }

    private void ensureNoForbiddenContent(String text) {
        if (text == null) {
            return;
        }
        String normalized = text.trim();
        if (normalized.isEmpty()) {
            return;
        }
        if (UNIX_ABSOLUTE_PATH.matcher(normalized).find()
                || WINDOWS_ABSOLUTE_PATH.matcher(normalized).find()
                || UNC_ABSOLUTE_PATH.matcher(normalized).find()
                || FILE_URI.matcher(normalized).find()) {
            throw new ScenarioAnalysisValidationException("Caminho absoluto detectado");
        }
        if (SECRET_PATTERN.matcher(normalized).find()) {
            throw new ScenarioAnalysisValidationException("Segredo aparente detectado");
        }
        if (CODE_PATTERN.matcher(normalized).find()) {
            throw new ScenarioAnalysisValidationException("Código indevido detectado");
        }
        if (COMMAND_PATTERN.matcher(normalized).find()) {
            throw new ScenarioAnalysisValidationException("Comando indevido detectado");
        }
    }
}
