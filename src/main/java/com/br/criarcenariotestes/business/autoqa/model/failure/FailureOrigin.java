package com.br.criarcenariotestes.business.autoqa.model.failure;

public enum FailureOrigin {
    TEST_CODE,
    APPLICATION,
    TEST_DATA,
    ENVIRONMENT,
    DEPENDENCY,
    CONFIGURATION,
    NETWORK,
    UNKNOWN
}