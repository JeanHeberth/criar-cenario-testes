package com.br.criarcenariotestes.business.autoqa.scenario;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class ScenarioInputSanitizer {

    private static final String REDACTED = "[REDACTED]";
    private static final Pattern KEY_VALUE_SECRET = Pattern.compile(
            "(?i)\\b(password|senha|passwd|token|access_token|refresh_token|api[_-]?key|apikey|secret|client_secret)\\b(\\s*[:=]\\s*)([^\\s,;\"'`]+)"
    );
    private static final Pattern AUTHORIZATION_BEARER = Pattern.compile(
            "(?i)\\b(authorization\\s*:?\\s*bearer\\s+)([^\\s,;\"'`]+)"
    );
    private static final Pattern URL_CREDENTIALS = Pattern.compile(
            "(?i)\\b((?:[a-z][a-z0-9+.-]*://)(?:[^\\s/?#:@]+:))([^\\s/?#@]+)(@)"
    );

    public String sanitize(String scenario) {
        Objects.requireNonNull(scenario, "scenario must not be null");

        String sanitized = scenario;
        sanitized = URL_CREDENTIALS.matcher(sanitized).replaceAll("$1" + REDACTED + "$3");
        sanitized = AUTHORIZATION_BEARER.matcher(sanitized).replaceAll("$1" + REDACTED);
        sanitized = KEY_VALUE_SECRET.matcher(sanitized).replaceAll("$1$2" + REDACTED);
        return sanitized;
    }
}
