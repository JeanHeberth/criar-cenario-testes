package com.br.criarcenariotestes.business.autoqa.model.review;

/**
 * Códigos estáveis usados pelo StaticReviewRuleEngine. A IA pode retornar outros
 * códigos livremente (campo "code" de ReviewIssue é String), mas os achados
 * determinísticos do motor estático sempre usam um destes valores via name().
 */
public enum ReviewRule {
    PLAN_FILE_MISSING,
    PLAN_FILE_EXTRA,
    PLAN_OPERATION_MISMATCH,
    PLAN_COMPONENT_MISMATCH,
    FRAMEWORK_MISMATCH,
    LANGUAGE_MISMATCH,
    INVALID_EXTENSION,
    ABSOLUTE_PATH,
    PATH_TRAVERSAL,
    MARKDOWN_FENCE,
    HARDCODED_SECRET,
    HARDCODED_CREDENTIAL,
    HARDCODED_URL,
    SLEEP_USAGE,
    THREAD_SLEEP,
    FIXED_WAIT,
    FRAGILE_SELECTOR,
    DUPLICATED_CODE,
    UNUSED_IMPORT,
    WILDCARD_IMPORT,
    MISSING_ASSERTION,
    GENERIC_EXCEPTION,
    EMPTY_TEST,
    EMPTY_METHOD,
    INVALID_REUSE,
    UNKNOWN_DEPENDENCY,
    UNPLANNED_DEPENDENCY,
    NAMING_CONVENTION_MISMATCH,
    MULTIPLE_RESPONSIBILITIES,
    FILE_TOO_LARGE,
    METHOD_TOO_LARGE,
    MISSING_ERROR_HANDLING,
    INCONSISTENT_STYLE,
    UNSUPPORTED_API,
    CONTENT_INTEGRITY_MISMATCH,
    UNKNOWN
}
