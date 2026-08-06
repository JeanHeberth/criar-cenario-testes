package com.br.criarcenariotestes.business.autoqa.execution;

import com.br.criarcenariotestes.business.autoqa.model.execution.ExecutionCommandId;
import com.br.criarcenariotestes.business.autoqa.model.execution.TestExecutionSummary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrai um resumo determinístico de stdout por ferramenta, quando possível.
 * Nunca lê relatórios do disco nesta fase — apenas stdout/stderr já
 * capturados. Quando o formato não é reconhecido, retorna lista vazia (nunca
 * inventa contagem); ExecutionSummaryBuilder decide se adiciona o warning
 * RESULT_PARSE_FAILED.
 */
@Component
public class ExecutionResultParser {

    private static final int MAX_FAILED_TESTS = 20;

    private static final Pattern PLAYWRIGHT_PASSED = Pattern.compile("(?m)^\\s*(\\d+)\\s+passed\\b");
    private static final Pattern PLAYWRIGHT_FAILED = Pattern.compile("(?m)^\\s*(\\d+)\\s+failed\\b");
    private static final Pattern PLAYWRIGHT_SKIPPED = Pattern.compile("(?m)^\\s*(\\d+)\\s+skipped\\b");
    private static final Pattern PLAYWRIGHT_FAILED_NAME = Pattern.compile("(?m)^\\s*[✘✗]\\s+(.+)$");

    private static final Pattern CYPRESS_PASSING = Pattern.compile("(?m)^\\s*(\\d+)\\s+passing\\b");
    private static final Pattern CYPRESS_FAILING = Pattern.compile("(?m)^\\s*(\\d+)\\s+failing\\b");
    private static final Pattern CYPRESS_PENDING = Pattern.compile("(?m)^\\s*(\\d+)\\s+pending\\b");

    private static final Pattern GRADLE_SUMMARY = Pattern.compile(
            "(\\d+)\\s+tests?\\s+completed(?:,\\s*(\\d+)\\s+failed)?(?:,\\s*(\\d+)\\s+skipped)?");

    private static final Pattern MAVEN_SUMMARY = Pattern.compile(
            "Tests run:\\s*(\\d+),\\s*Failures:\\s*(\\d+),\\s*Errors:\\s*(\\d+),\\s*Skipped:\\s*(\\d+)");

    private static final Pattern ROBOT_SUMMARY = Pattern.compile(
            "(\\d+)\\s+tests?,\\s*(\\d+)\\s+passed,\\s*(\\d+)\\s+failed");

    private static final Pattern PYTEST_PASSED = Pattern.compile("(\\d+)\\s+passed\\b");
    private static final Pattern PYTEST_FAILED = Pattern.compile("(\\d+)\\s+failed\\b");
    private static final Pattern PYTEST_SKIPPED = Pattern.compile("(\\d+)\\s+skipped\\b");
    private static final Pattern PYTEST_ERROR = Pattern.compile("(\\d+)\\s+error");
    private static final Pattern PYTEST_FAILED_NAME = Pattern.compile("(?m)^FAILED\\s+(\\S+)");

    public List<TestExecutionSummary> parse(ExecutionCommandId commandId, String stdout) {
        if (commandId == null || stdout == null || stdout.isBlank()) {
            return List.of();
        }
        return switch (commandId) {
            case PLAYWRIGHT_TEST, NPM_TEST, NPM_TEST_E2E -> parsePlaywright(stdout);
            case CYPRESS_RUN, CYPRESS_SCRIPT_RUN -> parseCypress(stdout);
            case GRADLE_WRAPPER_TEST, GRADLE_WRAPPER_CLEAN_TEST -> parseGradle(stdout);
            case MAVEN_WRAPPER_TEST, MAVEN_TEST -> parseMaven(stdout);
            case ROBOT_TEST -> parseRobot(stdout);
            case PYTEST -> parsePytest(stdout);
        };
    }

    private List<TestExecutionSummary> parsePlaywright(String stdout) {
        Integer passed = firstGroupAsInt(PLAYWRIGHT_PASSED, stdout);
        Integer failed = firstGroupAsInt(PLAYWRIGHT_FAILED, stdout);
        Integer skipped = firstGroupAsInt(PLAYWRIGHT_SKIPPED, stdout);
        if (passed == null && failed == null && skipped == null) {
            return List.of();
        }
        int p = orZero(passed), f = orZero(failed), s = orZero(skipped);
        return List.of(new TestExecutionSummary("PLAYWRIGHT", p + f + s, p, f, s, 0,
                extractNames(PLAYWRIGHT_FAILED_NAME, stdout), List.of()));
    }

    private List<TestExecutionSummary> parseCypress(String stdout) {
        Integer passing = firstGroupAsInt(CYPRESS_PASSING, stdout);
        Integer failing = firstGroupAsInt(CYPRESS_FAILING, stdout);
        Integer pending = firstGroupAsInt(CYPRESS_PENDING, stdout);
        if (passing == null && failing == null && pending == null) {
            return List.of();
        }
        int p = orZero(passing), f = orZero(failing), s = orZero(pending);
        return List.of(new TestExecutionSummary("CYPRESS", p + f + s, p, f, s, 0, List.of(), List.of()));
    }

    private List<TestExecutionSummary> parseGradle(String stdout) {
        MatchResult match = lastMatch(GRADLE_SUMMARY, stdout);
        if (match == null) {
            return List.of();
        }
        int total = toInt(match.group(1));
        int failed = match.group(2) != null ? toInt(match.group(2)) : 0;
        int skipped = match.group(3) != null ? toInt(match.group(3)) : 0;
        int passed = Math.max(0, total - failed - skipped);
        return List.of(new TestExecutionSummary("GRADLE", total, passed, failed, skipped, 0, List.of(), List.of()));
    }

    private List<TestExecutionSummary> parseMaven(String stdout) {
        MatchResult match = lastMatch(MAVEN_SUMMARY, stdout);
        if (match == null) {
            return List.of();
        }
        int total = toInt(match.group(1));
        int failures = toInt(match.group(2));
        int errors = toInt(match.group(3));
        int skipped = toInt(match.group(4));
        int passed = Math.max(0, total - failures - errors - skipped);
        return List.of(new TestExecutionSummary("MAVEN", total, passed, failures, skipped, errors, List.of(), List.of()));
    }

    private List<TestExecutionSummary> parseRobot(String stdout) {
        MatchResult match = lastMatch(ROBOT_SUMMARY, stdout);
        if (match == null) {
            return List.of();
        }
        int total = toInt(match.group(1));
        int passed = toInt(match.group(2));
        int failed = toInt(match.group(3));
        int skipped = Math.max(0, total - passed - failed);
        return List.of(new TestExecutionSummary("ROBOT", total, passed, failed, skipped, 0, List.of(), List.of()));
    }

    private List<TestExecutionSummary> parsePytest(String stdout) {
        Integer passed = lastGroupAsInt(PYTEST_PASSED, stdout);
        Integer failed = lastGroupAsInt(PYTEST_FAILED, stdout);
        Integer skipped = lastGroupAsInt(PYTEST_SKIPPED, stdout);
        Integer errors = lastGroupAsInt(PYTEST_ERROR, stdout);
        if (passed == null && failed == null && skipped == null && errors == null) {
            return List.of();
        }
        int p = orZero(passed), f = orZero(failed), s = orZero(skipped), e = orZero(errors);
        return List.of(new TestExecutionSummary("PYTEST", p + f + s + e, p, f, s, e,
                extractNames(PYTEST_FAILED_NAME, stdout), List.of()));
    }

    private Integer firstGroupAsInt(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? toInt(matcher.group(1)) : null;
    }

    private Integer lastGroupAsInt(Pattern pattern, String text) {
        MatchResult match = lastMatch(pattern, text);
        return match != null ? toInt(match.group(1)) : null;
    }

    private MatchResult lastMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        MatchResult last = null;
        while (matcher.find()) {
            last = matcher.toMatchResult();
        }
        return last;
    }

    private List<String> extractNames(Pattern pattern, String text) {
        List<String> names = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find() && names.size() < MAX_FAILED_TESTS) {
            names.add(matcher.group(1).trim());
        }
        return List.copyOf(names);
    }

    private int toInt(String value) {
        return Integer.parseInt(value);
    }

    private int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
