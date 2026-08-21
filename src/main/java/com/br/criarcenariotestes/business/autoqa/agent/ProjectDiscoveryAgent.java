package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.discovery.ProjectDiscoveryService;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Order(0)
public class ProjectDiscoveryAgent implements AutoQaAgent {

    private static final Logger log = LoggerFactory.getLogger(ProjectDiscoveryAgent.class);

    private final ProjectDiscoveryService projectDiscoveryService;

    public ProjectDiscoveryAgent(ProjectDiscoveryService projectDiscoveryService) {
        this.projectDiscoveryService = Objects.requireNonNull(projectDiscoveryService, "projectDiscoveryService must not be null");
    }

    @Override
    public String getName() {
        return "project-discovery";
    }

    @Override
    public AgentExecutionResult execute(AutoQaContext context) {
        Objects.requireNonNull(context, "context must not be null");
        try {
            ProjectDiscoveryResult result = projectDiscoveryService.discover(Path.of(context.getProjectPath()));
            result = aplicarFrameworkInformado(result, context);
            context.registerProjectDiscovery(result);
            return AgentExecutionResult.success(buildSummary(result));
        } catch (RuntimeException exception) {
            return AgentExecutionResult.failure("Falha na descoberta do projeto: " + exception.getMessage());
        }
    }

    /**
     * Preenche o framework quando o projeto não deu o que detectar — pasta nova,
     * ou build com só JUnit e nenhuma dependência de automação. Sem isto a
     * geração era barrada com "unsupported-framework" antes de qualquer chamada
     * de IA, e não havia como o usuário destravar.
     *
     * Só completa o que falta: framework detectado no projeto continua vencendo
     * o informado, porque é fato observado contra intenção declarada — e um
     * projeto que já usa Playwright não deve gerar Cypress por engano de
     * formulário.
     */
    private ProjectDiscoveryResult aplicarFrameworkInformado(ProjectDiscoveryResult result, AutoQaContext context) {
        AutomationFramework informado = context.getInformedAutomationFramework();
        if (informado == null || informado == AutomationFramework.UNKNOWN) {
            return result;
        }
        if (result.getAutomationFramework() != AutomationFramework.UNKNOWN) {
            if (result.getAutomationFramework() != informado) {
                log.warn("Framework informado difere do detectado; prevalece o detectado. "
                                + "executionId={}, informado={}, detectado={}",
                        context.getExecutionId(), informado, result.getAutomationFramework());
            }
            return result;
        }

        log.info("Framework não detectado no projeto; usando o informado. executionId={}, framework={}",
                context.getExecutionId(), informado);
        List<String> warnings = new ArrayList<>(result.getWarnings());
        warnings.add("Framework " + informado + " informado pelo usuário — não foi detectado no projeto");

        return new ProjectDiscoveryResult(
                result.getNormalizedProjectPath(), informado,
                result.getLanguage() == AutomationLanguage.UNKNOWN ? linguagemDe(informado) : result.getLanguage(),
                result.getPackageManager(), result.getBuildTool(), result.getTestingFrameworks(),
                result.getDetectedFrameworks(), result.getLibraries(), result.getConfigurationFile(),
                result.getEvidenceFiles(), warnings, result.getConfidence(), result.isValid());
    }

    /** Linguagem natural do framework, usada só quando o projeto não revelou a dele. */
    private AutomationLanguage linguagemDe(AutomationFramework framework) {
        return switch (framework) {
            case PLAYWRIGHT, CYPRESS -> AutomationLanguage.TYPESCRIPT;
            case SELENIDE, SELENIUM, REST_ASSURED, KARATE, PACT -> AutomationLanguage.JAVA;
            case ROBOT_FRAMEWORK -> AutomationLanguage.PYTHON;
            default -> AutomationLanguage.UNKNOWN;
        };
    }

    private String buildSummary(ProjectDiscoveryResult result) {
        return "Projeto descoberto: "
                + result.getAutomationFramework() + " / "
                + result.getLanguage() + " / "
                + result.getPackageManager();
    }
}
