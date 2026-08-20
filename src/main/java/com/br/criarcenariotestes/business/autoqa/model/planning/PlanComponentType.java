package com.br.criarcenariotestes.business.autoqa.model.planning;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum PlanComponentType {
    TEST, PAGE_OBJECT, COMPONENT_OBJECT, FIXTURE, HELPER, UTILITY,
    API_CLIENT, SERVICE, MODEL, DTO, FACTORY, BUILDER, RESOURCE,
    KEYWORD, VARIABLE_FILE, CONFIGURATION, @JsonEnumDefaultValue UNKNOWN
}
