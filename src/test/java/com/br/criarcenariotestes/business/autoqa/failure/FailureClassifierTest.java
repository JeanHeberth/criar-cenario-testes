package com.br.criarcenariotestes.business.autoqa.failure;

import com.br.criarcenariotestes.business.autoqa.model.failure.FailureEvidence;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FailureClassifierTest {

    @Test
    void shouldClassifyAssertionFailure() {
        FailureEvidence e = new FailureEvidence("stderr", null, null, "t1", "java.lang.AssertionError: expected <1> but was <2>", "excerpt", true);
        FailureClassifier c = new FailureClassifier();
        var f = c.classify(List.of(e));
        assertThat(f).isNotEmpty();
        assertThat(f.get(0).category()).isEqualTo(com.br.criarcenariotestes.business.autoqa.model.failure.FailureCategory.ASSERTION_FAILURE);
    }
}