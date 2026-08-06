package com.br.criarcenariotestes.business.autoqa.learning;

import com.br.criarcenariotestes.business.autoqa.model.learning.LearningEvidence;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningItem;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningSource;
import com.br.criarcenariotestes.business.autoqa.model.learning.LearningWarning;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deduplica LearningItem por id (que já codifica type+scope+título
 * normalizado+componentes/arquivos relacionados). Determinístico sempre
 * vence IA; entre dois determinísticos, maior confidence vence. Nunca
 * modifica os itens originais — sempre produz uma coleção nova. Conflito
 * semântico real (reusable ou positive divergentes) nunca é descartado
 * silenciosamente: vira LearningWarning.
 */
public class LearningDeduplicator {

    public record Result(List<LearningItem> items, List<LearningWarning> warnings) {
        public Result {
            items = items == null ? List.of() : List.copyOf(items);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    public Result deduplicate(List<LearningItem> deterministicItems, List<LearningItem> aiItems) {
        Objects.requireNonNull(deterministicItems, "deterministicItems must not be null");
        Objects.requireNonNull(aiItems, "aiItems must not be null");

        Map<String, LearningItem> byId = new LinkedHashMap<>();
        List<LearningWarning> warnings = new ArrayList<>();

        List<LearningItem> combined = new ArrayList<>(deterministicItems.size() + aiItems.size());
        combined.addAll(deterministicItems);
        combined.addAll(aiItems);

        for (LearningItem candidate : combined) {
            LearningItem existing = byId.get(candidate.id());
            if (existing == null) {
                byId.put(candidate.id(), candidate);
                continue;
            }
            if (conflicts(existing, candidate)) {
                warnings.add(new LearningWarning("CONFLICTING_EVIDENCE",
                        "Conflito entre aprendizados com o mesmo identificador semântico: " + candidate.id(), false));
            }
            LearningItem winner = pickWinner(existing, candidate);
            LearningItem merged = mergeEvidence(winner, existing, candidate);
            byId.put(candidate.id(), merged);
        }

        return new Result(List.copyOf(byId.values()), List.copyOf(warnings));
    }

    private boolean conflicts(LearningItem a, LearningItem b) {
        return a.reusable() != b.reusable() || a.positive() != b.positive();
    }

    private LearningItem pickWinner(LearningItem existing, LearningItem candidate) {
        boolean existingIsAi = existing.source() == LearningSource.AI_SUGGESTION;
        boolean candidateIsAi = candidate.source() == LearningSource.AI_SUGGESTION;
        if (existingIsAi && !candidateIsAi) {
            return candidate;
        }
        if (!existingIsAi && candidateIsAi) {
            return existing;
        }
        // ambos determinísticos (ou ambos IA): maior confidence vence; empate mantém o primeiro visto
        return candidate.confidence().ordinal() < existing.confidence().ordinal() ? candidate : existing;
    }

    private LearningItem mergeEvidence(LearningItem winner, LearningItem a, LearningItem b) {
        LinkedHashSet<LearningEvidence> merged = new LinkedHashSet<>(a.evidence());
        merged.addAll(b.evidence());
        if (merged.size() == winner.evidence().size()) {
            return winner;
        }
        return new LearningItem(winner.id(), winner.type(), winner.scope(), winner.source(), winner.title(),
                winner.description(), winner.recommendation(), winner.confidence(), winner.approvalStatus(),
                List.copyOf(merged), winner.relatedComponents(), winner.relatedFiles(), winner.relatedFailureCodes(),
                winner.tags(), winner.positive(), winner.reusable(), winner.humanReviewRequired());
    }
}
