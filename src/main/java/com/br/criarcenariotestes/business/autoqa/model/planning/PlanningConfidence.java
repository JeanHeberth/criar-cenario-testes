package com.br.criarcenariotestes.business.autoqa.model.planning;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum PlanningConfidence {
    HIGH, MEDIUM, LOW, @JsonEnumDefaultValue UNKNOWN
}
