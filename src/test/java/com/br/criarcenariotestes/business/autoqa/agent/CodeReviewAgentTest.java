package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedCodeResponse;
import com.br.criarcenariotestes.business.autoqa.model.context.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.enums.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.request.AutoQaRequest;
import com.br.criarcenariotestes.business.autoqa.workflow.AutoQaContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CodeReviewAgent")
class CodeReviewAgentTest {

    private CodeReviewAgent agent;

    @BeforeEach
    void setUp() {
        agent = new CodeReviewAgent();
    }

    private AutoQaContext buildContextWithFiles(List<GeneratedFile> files) {
        AutoQaRequest req = new AutoQaRequest(
                "Title",
                null,
                "Scenario text",
                ".",
                null,
                null,
                null,
                null,
                false,
                false
        );
        AutoQaContext ctx = new AutoQaContext(req);
        GeneratedCodeResponse resp = new GeneratedCodeResponse(
                files, List.of(), List.of(), List.of(), "summary", false, null
        );
        ctx.setGeneratedCodeResponse(resp);
        return ctx;
    }

    @Test
    @DisplayName("deve adicionar warning para console.log e não adicionar NO_ASSERTION se houver expect")
    void warnsOnConsoleLog() {
        GeneratedFile f = new GeneratedFile("tests/login.spec.ts", GeneratedFileOperation.CREATE,
                "console.log('debug');\nexpect(true).toBe(true);", "", null);
        AutoQaContext ctx = buildContextWithFiles(List.of(f));

        agent.review(ctx);

        assertThat(ctx.getIssues()).anyMatch(i -> "CONSOLE_LOG".equals(i.code()));
        assertThat(ctx.getIssues()).noneMatch(i -> "NO_ASSERTION".equals(i.code()));
    }

    @Test
    @DisplayName("deve adicionar warning para ausência de asserções")
    void warnsOnNoAssertion() {
        GeneratedFile f = new GeneratedFile("tests/noassert.spec.ts", GeneratedFileOperation.CREATE,
                "import { test } from '@playwright/test';\ntest('ok', async () => { /* no checks */ });", "", null);
        AutoQaContext ctx = buildContextWithFiles(List.of(f));

        agent.review(ctx);

        assertThat(ctx.getIssues()).anyMatch(i -> "NO_ASSERTION".equals(i.code()));
    }

    @Test
    @DisplayName("não adiciona issues para arquivo limpo")
    void noIssuesForCleanFile() {
        GeneratedFile f = new GeneratedFile("tests/clean.spec.ts", GeneratedFileOperation.CREATE,
                "import { test, expect } from '@playwright/test';\n test('ok', async () => { expect(1).toBe(1); });", "", null);
        AutoQaContext ctx = buildContextWithFiles(List.of(f));

        agent.review(ctx);

        assertThat(ctx.getIssues()).isEmpty();
    }
}
