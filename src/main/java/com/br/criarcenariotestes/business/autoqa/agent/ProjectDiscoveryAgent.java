package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.framework.AutomationFrameworkAdapter;
import com.br.criarcenariotestes.business.autoqa.framework.AutomationFrameworkResolver;
import com.br.criarcenariotestes.business.autoqa.model.context.AllowedCommand;
import com.br.criarcenariotestes.business.autoqa.model.context.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.PackageManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Agente de descoberta do projeto de automação.
 * Toda detecção é determinística — sem uso de IA nesta etapa.
 * A IA é usada apenas nas fases de análise e geração.
 */
@Component
@RequiredArgsConstructor
public class ProjectDiscoveryAgent {

    private static final Logger log = LoggerFactory.getLogger(ProjectDiscoveryAgent.class);

    private final AutomationFrameworkResolver resolver;

    public ProjectDiscoveryResult discover(
            Path projectPath,
            AutomationFramework informedFramework,
            AutomationLanguage informedLanguage
    ) {
        log.info("Iniciando descoberta do projeto. path='{}'", projectPath);

        List<String> evidences = new ArrayList<>();
        List<String> divergences = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        AutomationFramework detected = detectFramework(projectPath, evidences, warnings);
        AutomationLanguage detectedLang = detectLanguage(projectPath, evidences, warnings);
        PackageManager packageManager = detectPackageManager(projectPath, evidences);
        String configFile = findConfigurationFile(projectPath, detected);
        List<AllowedCommand> suggestedCommands = buildSuggestedCommands(
                detected, detectedLang, packageManager, configFile
        );

        if (informedFramework != null
                && informedFramework != AutomationFramework.UNKNOWN
                && detected != AutomationFramework.UNKNOWN
                && informedFramework != detected) {
            String divergence = "Framework informado (" + informedFramework.getDescricao()
                    + ") diverge do framework detectado (" + detected.getDescricao() + ")";
            divergences.add(divergence);
            log.warn("Divergência de framework detectada. {}", divergence);
        }

        ProjectDiscoveryResult result = ProjectDiscoveryResult.builder()
                .informedFramework(informedFramework)
                .detectedFramework(detected)
                .informedLanguage(informedLanguage)
                .detectedLanguage(detectedLang)
                .packageManager(packageManager)
                .configurationFile(configFile)
                .suggestedCommands(suggestedCommands)
                .detectionEvidences(evidences)
                .divergences(divergences)
                .warnings(warnings)
                .build();

        log.info("Descoberta concluída. framework={}, language={}, packageManager={}, divergences={}",
                detected, detectedLang, packageManager, divergences.size());

        return result;
    }

    private AutomationFramework detectFramework(Path projectPath, List<String> evidences, List<String> warnings) {
        // Playwright — config files (prioridade sobre Cypress)
        for (String cfg : List.of("playwright.config.ts", "playwright.config.js",
                "playwright.config.mts", "playwright.config.mjs")) {
            if (exists(projectPath, cfg)) {
                evidences.add("Arquivo de configuração encontrado: " + cfg);
                return AutomationFramework.PLAYWRIGHT;
            }
        }

        // Cypress — config files
        for (String cfg : List.of("cypress.config.ts", "cypress.config.js")) {
            if (exists(projectPath, cfg)) {
                evidences.add("Arquivo de configuração encontrado: " + cfg);
                return AutomationFramework.CYPRESS;
            }
        }

        // Inferência via package.json
        Path packageJson = projectPath.resolve("package.json");
        if (Files.exists(packageJson)) {
            try {
                String content = Files.readString(packageJson);
                if (content.contains("\"@playwright/test\"")) {
                    evidences.add("Dependência @playwright/test encontrada no package.json");
                    return AutomationFramework.PLAYWRIGHT;
                }
                if (content.contains("\"cypress\"")) {
                    evidences.add("Dependência cypress encontrada no package.json");
                    return AutomationFramework.CYPRESS;
                }
            } catch (IOException ex) {
                warnings.add("Não foi possível ler o package.json para detecção de framework");
            }
        }

        warnings.add("Framework não detectado automaticamente — informe o framework manualmente");
        return AutomationFramework.UNKNOWN;
    }

    private AutomationLanguage detectLanguage(Path projectPath, List<String> evidences, List<String> warnings) {
        if (exists(projectPath, "tsconfig.json")) {
            evidences.add("tsconfig.json encontrado");
            return AutomationLanguage.TYPESCRIPT;
        }

        try (var stream = Files.list(projectPath)) {
            boolean hasTs = stream.anyMatch(p -> p.toString().endsWith(".ts"));
            if (hasTs) {
                evidences.add("Arquivo .ts encontrado na raiz do projeto");
                return AutomationLanguage.TYPESCRIPT;
            }
        } catch (IOException ex) {
            log.warn("Erro ao listar arquivos para detecção de linguagem em: {}", projectPath);
        }

        try (var stream = Files.list(projectPath)) {
            boolean hasJs = stream.anyMatch(p -> p.toString().endsWith(".js")
                    && !p.getFileName().toString().startsWith("playwright.config")
                    && !p.getFileName().toString().startsWith("cypress.config"));
            if (hasJs) {
                evidences.add("Arquivo .js encontrado na raiz do projeto");
                return AutomationLanguage.JAVASCRIPT;
            }
        } catch (IOException ex) {
            log.warn("Erro ao listar arquivos .js em: {}", projectPath);
        }

        warnings.add("Linguagem não detectada automaticamente — informe a linguagem manualmente");
        return AutomationLanguage.UNKNOWN;
    }

    private PackageManager detectPackageManager(Path projectPath, List<String> evidences) {
        if (exists(projectPath, "package-lock.json")) {
            evidences.add("package-lock.json encontrado → NPM");
            return PackageManager.NPM;
        }
        if (exists(projectPath, "yarn.lock")) {
            evidences.add("yarn.lock encontrado → YARN");
            return PackageManager.YARN;
        }
        if (exists(projectPath, "pnpm-lock.yaml")) {
            evidences.add("pnpm-lock.yaml encontrado → PNPM");
            return PackageManager.PNPM;
        }
        return PackageManager.UNKNOWN;
    }

    private String findConfigurationFile(Path projectPath, AutomationFramework framework) {
        if (framework == AutomationFramework.PLAYWRIGHT) {
            for (String cfg : List.of("playwright.config.ts", "playwright.config.js",
                    "playwright.config.mts", "playwright.config.mjs")) {
                if (exists(projectPath, cfg)) return cfg;
            }
        }
        if (framework == AutomationFramework.CYPRESS) {
            for (String cfg : List.of("cypress.config.ts", "cypress.config.js")) {
                if (exists(projectPath, cfg)) return cfg;
            }
        }
        return null;
    }

    private List<AllowedCommand> buildSuggestedCommands(
            AutomationFramework framework,
            AutomationLanguage language,
            PackageManager packageManager,
            String configFile
    ) {
        if (framework == AutomationFramework.UNKNOWN) {
            return List.of();
        }
        try {
            AutomationFrameworkAdapter adapter = resolver.resolve(framework);
            ProjectDiscoveryResult partial = ProjectDiscoveryResult.builder()
                    .detectedFramework(framework)
                    .detectedLanguage(language)
                    .packageManager(packageManager)
                    .configurationFile(configFile)
                    .build();
            return adapter.validationCommands(partial);
        } catch (Exception ex) {
            log.warn("Não foi possível montar comandos sugeridos para framework={}: {}", framework, ex.getMessage());
            return List.of();
        }
    }

    private boolean exists(Path base, String fileName) {
        return Files.exists(base.resolve(fileName));
    }
}
