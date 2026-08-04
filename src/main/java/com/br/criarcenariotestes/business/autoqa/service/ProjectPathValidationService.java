package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.exception.InvalidProjectPathException;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.enums.PackageManager;
import com.br.criarcenariotestes.business.autoqa.model.response.ProjectValidationResponse;
import com.br.criarcenariotestes.business.autoqa.properties.AutoQaProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Valida e normaliza o caminho do projeto de automação.
 * <p>
 * Utiliza exclusivamente java.nio.file.Path — sem concatenação manual de barras.
 * Nunca expõe informações sobre outros diretórios do sistema.
 */
@Service
@RequiredArgsConstructor
public class ProjectPathValidationService {

    private static final Logger log = LoggerFactory.getLogger(ProjectPathValidationService.class);

    // Diretórios raiz de sistema que nunca são permitidos
    private static final Set<String> FORBIDDEN_ROOTS = Set.of(
            "/",
            "C:\\",
            "C:/",
            "/root"
    );

    // Nomes de diretório que indicam raiz pessoal — comparação parcial
    private static final List<String> FORBIDDEN_HOME_PATTERNS = List.of(
            "/Users/",
            "/home/"
    );

    private final AutoQaProperties autoQaProperties;

    /**
     * Valida o caminho e retorna o resultado da validação.
     * Não lança exceção — retorna invalid com mensagem clara.
     */
    public ProjectValidationResponse validate(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return ProjectValidationResponse.invalid("O caminho do projeto não pode ser vazio");
        }

        Path normalizedPath;
        try {
            normalizedPath = resolveSafely(rawPath);
        } catch (InvalidProjectPathException ex) {
            return ProjectValidationResponse.invalid(ex.getReason());
        }

        List<String> warnings = new ArrayList<>();

        if (!Files.exists(normalizedPath)) {
            return ProjectValidationResponse.invalid(
                    "O caminho não existe no sistema de arquivos: " + normalizedPath
            );
        }

        if (!Files.isDirectory(normalizedPath)) {
            return ProjectValidationResponse.invalid(
                    "O caminho informado não é um diretório: " + normalizedPath
            );
        }

        boolean readable = Files.isReadable(normalizedPath);
        boolean writable = Files.isWritable(normalizedPath);

        if (!readable) {
            return ProjectValidationResponse.invalid(
                    "Sem permissão de leitura no diretório: " + normalizedPath
            );
        }

        if (!writable) {
            warnings.add("Sem permissão de escrita. Não será possível aplicar arquivos gerados");
        }

        if (autoQaProperties.hasAllowedRoots()) {
            boolean withinAllowedRoot = autoQaProperties.getAllowedRoots().stream()
                    .map(root -> Paths.get(root).toAbsolutePath().normalize())
                    .anyMatch(normalizedPath::startsWith);

            if (!withinAllowedRoot) {
                return ProjectValidationResponse.invalid(
                        "O caminho está fora das raízes permitidas configuradas"
                );
            }
        }

        // Detecta framework e linguagem sem IA
        AutomationFramework detectedFramework = detectFramework(normalizedPath, warnings);
        AutomationLanguage detectedLanguage = detectLanguage(normalizedPath, warnings);
        PackageManager packageManager = detectPackageManager(normalizedPath);
        String configurationFile = detectConfigurationFile(normalizedPath, detectedFramework);

        log.info("Caminho validado com sucesso. path='{}', framework={}, language={}, packageManager={}, readable={}, writable={}",
                normalizedPath, detectedFramework, detectedLanguage, packageManager, readable, writable);

        return ProjectValidationResponse.builder()
                .valid(true)
                .normalizedPath(normalizedPath.toString())
                .readable(readable)
                .writable(writable)
                .detectedFramework(detectedFramework)
                .detectedLanguage(detectedLanguage)
                .packageManager(packageManager)
                .configurationFile(configurationFile)
                .warnings(warnings)
                .build();
    }

    /**
     * Resolve o caminho de forma segura, bloqueando path traversal e raízes proibidas.
     * Lança InvalidProjectPathException em caso de violação.
     */
    public Path resolveSafely(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw InvalidProjectPathException.empty();
        }

        Path resolved;
        try {
            resolved = Path.of(rawPath).toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            throw new InvalidProjectPathException(
                    "Caminho inválido: " + rawPath
            );
        }

        // Bloqueia raízes de sistema
        String pathStr = resolved.toString();
        if (FORBIDDEN_ROOTS.contains(pathStr) || FORBIDDEN_ROOTS.contains(pathStr + "\\")) {
            throw InvalidProjectPathException.forbiddenRoot(pathStr);
        }

        // Bloqueia diretório pessoal inteiro (ex: /Users/joao sem subdiretório)
        for (String pattern : FORBIDDEN_HOME_PATTERNS) {
            if (pathStr.startsWith(pattern)) {
                // Permite sub-diretórios: /Users/joao/projetos mas não /Users/joao
                int depth = resolved.getNameCount();
                // /Users/joao tem nameCount=2 (Users, joao) — exige ao menos 3
                if (depth <= 2) {
                    throw InvalidProjectPathException.forbiddenRoot(pathStr);
                }
            }
        }

        // Detecta path traversal via comparação de prefixo após normalização
        if (rawPath.contains("..")) {
            // Permitido se normalização resultou em caminho dentro do sistema
            // mas bloqueia se contém ".." que escape para raíz
            Path parent = resolved.getParent();
            if (parent == null || pathStr.equals("/") || pathStr.matches("^[A-Za-z]:\\\\?$")) {
                throw InvalidProjectPathException.pathTraversal();
            }
        }

        return resolved;
    }

    /**
     * Detecta o framework de automação de forma determinística, sem IA.
     * Verifica a presença de arquivos de configuração conhecidos.
     */
    private AutomationFramework detectFramework(Path projectPath, List<String> warnings) {
        // Playwright
        if (existsInProject(projectPath, "playwright.config.ts")
                || existsInProject(projectPath, "playwright.config.js")
                || existsInProject(projectPath, "playwright.config.mts")
                || existsInProject(projectPath, "playwright.config.mjs")) {
            return AutomationFramework.PLAYWRIGHT;
        }

        // Cypress
        if (existsInProject(projectPath, "cypress.config.ts")
                || existsInProject(projectPath, "cypress.config.js")) {
            return AutomationFramework.CYPRESS;
        }

        // Tenta inferir via package.json
        Path packageJson = projectPath.resolve("package.json");
        if (Files.exists(packageJson)) {
            try {
                String content = Files.readString(packageJson);
                if (content.contains("\"@playwright/test\"")) {
                    return AutomationFramework.PLAYWRIGHT;
                }
                if (content.contains("\"cypress\"")) {
                    return AutomationFramework.CYPRESS;
                }
            } catch (IOException ex) {
                warnings.add("Não foi possível ler package.json para detecção de framework");
                log.warn("Erro ao ler package.json em: {}", packageJson);
            }
        }

        warnings.add("Framework não detectado automaticamente — informe manualmente");
        return AutomationFramework.UNKNOWN;
    }

    /**
     * Detecta a linguagem de automação de forma determinística, sem IA.
     */
    private AutomationLanguage detectLanguage(Path projectPath, List<String> warnings) {
        if (existsInProject(projectPath, "tsconfig.json")) {
            return AutomationLanguage.TYPESCRIPT;
        }

        // Verifica se há arquivos .ts na raiz
        try (var stream = Files.list(projectPath)) {
            boolean hasTs = stream.anyMatch(p -> p.toString().endsWith(".ts"));
            if (hasTs) {
                return AutomationLanguage.TYPESCRIPT;
            }
        } catch (IOException ex) {
            log.warn("Erro ao listar arquivos para detecção de linguagem em: {}", projectPath);
        }

        // Fallback: verifica .js
        try (var stream = Files.list(projectPath)) {
            boolean hasJs = stream.anyMatch(p -> p.toString().endsWith(".js"));
            if (hasJs) {
                return AutomationLanguage.JAVASCRIPT;
            }
        } catch (IOException ex) {
            log.warn("Erro ao listar arquivos .js em: {}", projectPath);
        }

        warnings.add("Linguagem não detectada automaticamente — informe manualmente");
        return AutomationLanguage.UNKNOWN;
    }

    private boolean existsInProject(Path projectPath, String fileName) {
        return Files.exists(projectPath.resolve(fileName));
    }

    private PackageManager detectPackageManager(Path projectPath) {
        if (existsInProject(projectPath, "package-lock.json")) {
            return PackageManager.NPM;
        }
        if (existsInProject(projectPath, "yarn.lock")) {
            return PackageManager.YARN;
        }
        if (existsInProject(projectPath, "pnpm-lock.yaml")) {
            return PackageManager.PNPM;
        }
        return PackageManager.UNKNOWN;
    }

    private String detectConfigurationFile(Path projectPath, AutomationFramework framework) {
        if (framework == AutomationFramework.PLAYWRIGHT) {
            for (String cfg : List.of("playwright.config.ts", "playwright.config.js",
                    "playwright.config.mts", "playwright.config.mjs")) {
                if (existsInProject(projectPath, cfg)) {
                    return cfg;
                }
            }
        }
        if (framework == AutomationFramework.CYPRESS) {
            for (String cfg : List.of("cypress.config.ts", "cypress.config.js")) {
                if (existsInProject(projectPath, cfg)) {
                    return cfg;
                }
            }
        }
        return null;
    }
}
