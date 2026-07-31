package com.br.criarcenariotestes.business.autoqa.model.context;

import java.util.List;

public record CodeReviewResult(
        boolean approved,
        List<CodeReviewIssue> issues,
        List<String> suggestions,
        int revisionNumber
) {
}
