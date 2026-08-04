package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.discovery.ProjectDiscoveryService;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Objects;

@Component
@Order(0)
public class ProjectDiscoveryAgent implements AutoQaAgent {

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
            context.registerProjectDiscovery(result);
            return AgentExecutionResult.success(buildSummary(result));
        } catch (RuntimeException exception) {
            return AgentExecutionResult.failure("Falha na descoberta do projeto: " + exception.getMessage());
        }
    }

    private String buildSummary(ProjectDiscoveryResult result) {
        return "Projeto descoberto: "
                + result.getAutomationFramework() + " / "
                + result.getLanguage() + " / "
                + result.getPackageManager();
    }
}
