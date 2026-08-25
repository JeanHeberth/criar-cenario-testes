package com.br.criarcenariotestes.business.autoqa.planning;

import com.br.criarcenariotestes.business.autoqa.security.PadroesDeConteudoProibido;

import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.*;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import com.br.criarcenariotestes.business.autoqa.planning.exception.PlanningValidationException;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class PlanningValidator {

    private static final Pattern CODE_PATTERN = PadroesDeConteudoProibido.CODIGO;
    private static final Pattern COMMAND_PATTERN = PadroesDeConteudoProibido.COMANDO;
    private static final Pattern PATH_TRAVERSAL = Pattern.compile("\\.\\./");
    private static final Pattern ABSOLUTE_UNIX = Pattern.compile("^/");
    private static final Pattern ABSOLUTE_WINDOWS = Pattern.compile("(?i)^[A-Za-z]:\\\\");
    private static final Pattern ABSOLUTE_UNC = Pattern.compile("^\\\\\\\\");
    private static final Pattern FILE_URI = Pattern.compile("(?i)^file://");

    /**
     * Remove do início dos caminhos o nome da PRÓPRIA pasta do projeto.
     *
     * <p>Observado em produção: com o projeto vazio, o plano saiu com
     * "testando-AUTO-QA/api/auth/authApiClient.ts" — o nome da raiz virou
     * prefixo. Como os caminhos são relativos à raiz, aplicar isso criaria
     * &lt;projeto&gt;/testando-AUTO-QA/api/... , o projeto aninhado dentro de si
     * mesmo. É sempre errado e a correção é única, então normalizar é melhor do
     * que derrubar a rodada inteira depois de a IA já ter sido paga.
     *
     * <p>Mesma família do caso ".claude/": o modelo decidindo estrutura de
     * diretório em vez de derivá-la. A diferença é que aqui a resposta certa é
     * calculável, e por isso o código a calcula.
     */
    private TechnicalPlanResult removerPrefixoRedundanteDaRaiz(TechnicalPlanResult result,
                                                                ProjectDiscoveryResult discovery) {
        if (discovery == null || discovery.getNormalizedProjectPath() == null) {
            return result;
        }
        java.nio.file.Path nomeDaRaiz = discovery.getNormalizedProjectPath().getFileName();
        if (nomeDaRaiz == null) {
            return result;
        }
        String prefixo = nomeDaRaiz.toString() + "/";

        boolean algumCorrigido = result.fileActions().stream()
                .anyMatch(a -> a != null && a.relativePath() != null && a.relativePath().startsWith(prefixo));
        if (!algumCorrigido) {
            return result;
        }

        List<PlannedFileAction> corrigidas = result.fileActions().stream()
                .map(a -> (a == null || a.relativePath() == null || !a.relativePath().startsWith(prefixo))
                        ? a
                        : new PlannedFileAction(a.relativePath().substring(prefixo.length()), a.operation(),
                                a.componentType(), a.reason(), a.existingFile(), a.required(),
                                a.approvalRequirement(), a.dependencies(), a.warnings()))
                .toList();

        return new TechnicalPlanResult(result.title(), result.strategy(), corrigidas, result.components(),
                result.reuseDecisions(), result.risks(), result.warnings(), result.assumptions(),
                result.constraints(), result.requiredApprovals(), result.status(), result.confidence(),
                result.valid());
    }

    public TechnicalPlanResult validate(TechnicalPlanResult result,
                                        ProjectDiscoveryResult discovery,
                                        ScenarioAnalysisResult scenario,
                                        ProjectKnowledgeResult knowledge) {
        if (result == null) throw new PlanningValidationException("result must not be null");
        if (result.title() == null || result.title().isBlank()) throw new PlanningValidationException("title ausente");
        if (result.strategy() == null || result.strategy().isBlank()) throw new PlanningValidationException("strategy ausente");
        if (result.fileActions() == null) throw new PlanningValidationException("fileActions ausente");

        result = removerPrefixoRedundanteDaRaiz(result, discovery);

        Set<String> existingPaths = knowledge != null && knowledge.components() != null
            ? knowledge.components().stream()
                .filter(Objects::nonNull)
                .map(c -> c.relativePath())
                .collect(Collectors.toSet())
            : Set.of();

        Set<String> seenPaths = new LinkedHashSet<>();
        for (int i = 0; i < result.fileActions().size(); i++) {
            PlannedFileAction action = result.fileActions().get(i);
            if (action == null) throw new PlanningValidationException("fileActions[" + i + "] nulo");
            validateFileAction(action, i, existingPaths);
            if (!seenPaths.add(action.relativePath())) {
                throw new PlanningValidationException("relativePath duplicado: " + action.relativePath());
            }
        }

        validateNoNullStrings(result.assumptions(), "assumptions");
        validateNoNullStrings(result.constraints(), "constraints");
        validateNoNullStrings(result.requiredApprovals(), "requiredApprovals");

        checkNoCodeOrCommands(result.title(), "title");
        checkNoCodeOrCommands(result.strategy(), "strategy");

        for (int i = 0; i < result.components().size(); i++) {
            PlannedComponent comp = result.components().get(i);
            if (comp == null) throw new PlanningValidationException("components[" + i + "] nulo");
        }

        for (int i = 0; i < result.reuseDecisions().size(); i++) {
            ReuseDecision rd = result.reuseDecisions().get(i);
            if (rd == null) throw new PlanningValidationException("reuseDecisions[" + i + "] nulo");
            if (rd.componentPath() == null || rd.componentPath().isBlank()) {
                throw new PlanningValidationException("reuseDecisions[" + i + "].componentPath ausente");
            }
            if (rd.reason() == null || rd.reason().isBlank()) {
                throw new PlanningValidationException("reuseDecisions[" + i + "].reason ausente");
            }
            if (rd.confidence() == null) {
                throw new PlanningValidationException("reuseDecisions[" + i + "].confidence ausente");
            }
            if (rd.reuse() && !existingPaths.contains(rd.componentPath())) {
                throw new PlanningValidationException("reuseDecisions[" + i + "].componentPath não existe no knowledge: " + rd.componentPath());
            }
        }

        for (int i = 0; i < result.risks().size(); i++) {
            if (result.risks().get(i) == null) throw new PlanningValidationException("risks[" + i + "] nulo");
        }
        for (int i = 0; i < result.warnings().size(); i++) {
            if (result.warnings().get(i) == null) throw new PlanningValidationException("warnings[" + i + "] nulo");
        }

        validateStatusCoherence(result);

        if (knowledge != null) {
            KnowledgeStatus knowledgeStatus = knowledge.status();
            if (knowledgeStatus == KnowledgeStatus.PARTIAL || knowledgeStatus == KnowledgeStatus.EMPTY) {
                if (result.warnings().isEmpty()) {
                    throw new PlanningValidationException("Plano com knowledge parcial ou vazio deve conter warnings");
                }
            }
        }

        return result;
    }

    private void validateFileAction(PlannedFileAction action, int i, Set<String> existingPaths) {
        String path = action.relativePath();
        if (path == null || path.isBlank()) throw new PlanningValidationException("fileActions[" + i + "].relativePath ausente");
        if (action.operation() == null) throw new PlanningValidationException("fileActions[" + i + "].operation ausente");
        if (action.componentType() == null) throw new PlanningValidationException("fileActions[" + i + "].componentType ausente");
        if (action.reason() == null || action.reason().isBlank()) throw new PlanningValidationException("fileActions[" + i + "].reason ausente");
        if (action.approvalRequirement() == null) throw new PlanningValidationException("fileActions[" + i + "].approvalRequirement ausente");

        if (PATH_TRAVERSAL.matcher(path).find()) throw new PlanningValidationException("Path traversal detectado: " + path);
        if (ABSOLUTE_UNIX.matcher(path).find()) throw new PlanningValidationException("Caminho absoluto Unix detectado: " + path);
        if (ABSOLUTE_WINDOWS.matcher(path).find()) throw new PlanningValidationException("Caminho absoluto Windows detectado: " + path);
        if (ABSOLUTE_UNC.matcher(path).find()) throw new PlanningValidationException("Caminho UNC detectado: " + path);
        if (FILE_URI.matcher(path).find()) throw new PlanningValidationException("File URI detectado: " + path);

        switch (action.operation()) {
            case REUSE -> {
                if (!existingPaths.contains(path)) throw new PlanningValidationException("REUSE para arquivo inexistente: " + path);
                if (!action.existingFile()) throw new PlanningValidationException("REUSE deve ter existingFile=true: " + path);
            }
            case UPDATE -> {
                if (!existingPaths.contains(path)) throw new PlanningValidationException("UPDATE para arquivo inexistente: " + path);
                if (!action.existingFile()) throw new PlanningValidationException("UPDATE deve ter existingFile=true: " + path);
                if (action.approvalRequirement() == null
                        || action.approvalRequirement().ordinal() < ApprovalRequirement.FILE_UPDATE_REQUIRED.ordinal()) {
                    throw new PlanningValidationException(
                        "UPDATE requer approvalRequirement FILE_UPDATE_REQUIRED ou mais restritivo (recebido: "
                        + action.approvalRequirement() + "): " + path);
                }
            }
            case CREATE -> {
                if (existingPaths.contains(path)) throw new PlanningValidationException("CREATE para arquivo existente: " + path);
                if (action.existingFile()) throw new PlanningValidationException("CREATE deve ter existingFile=false: " + path);
            }
            default -> { /* NONE is fine */ }
        }

        checkNoCodeOrCommands(action.reason(), "fileActions[" + i + "].reason");
    }

    private void validateStatusCoherence(TechnicalPlanResult result) {
        PlanningStatus status = result.status();
        if (status == null) return;

        switch (status) {
            case INVALID -> {
                if (result.valid()) throw new PlanningValidationException("INVALID deve ter valid=false");
            }
            case READY -> {
                if (!result.valid()) throw new PlanningValidationException("READY deve ter valid=true");
                boolean hasAction = !result.fileActions().isEmpty()
                    || result.reuseDecisions().stream().anyMatch(ReuseDecision::reuse);
                if (!hasAction) throw new PlanningValidationException("READY deve ter ao menos uma ação ou reutilização");
            }
            case READY_WITH_WARNINGS -> {
                if (!result.valid()) throw new PlanningValidationException("READY_WITH_WARNINGS deve ter valid=true");
                if (result.warnings().isEmpty()) throw new PlanningValidationException("READY_WITH_WARNINGS deve ter warnings");
            }
            case BLOCKED -> {
                boolean hasHumanDecision = result.warnings().stream().anyMatch(PlanningWarning::requiresHumanDecision);
                boolean hasBlockingRisk = result.risks().stream().anyMatch(PlanningRisk::blocking);
                if (!hasHumanDecision && !hasBlockingRisk) {
                    throw new PlanningValidationException("BLOCKED deve ter warning com requiresHumanDecision=true ou risk blocking=true");
                }
            }
        }
    }

    private void validateNoNullStrings(List<String> list, String fieldName) {
        if (list == null) return;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == null) throw new PlanningValidationException(fieldName + "[" + i + "] nulo");
        }
    }

    private void checkNoCodeOrCommands(String text, String fieldName) {
        if (text == null || text.isBlank()) return;
        if (CODE_PATTERN.matcher(text).find()) throw new PlanningValidationException("Código detectado em " + fieldName);
        if (COMMAND_PATTERN.matcher(text).find()) throw new PlanningValidationException("Comando detectado em " + fieldName);
    }
}
