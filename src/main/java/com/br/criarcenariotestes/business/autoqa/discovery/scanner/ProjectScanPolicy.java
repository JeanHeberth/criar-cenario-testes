package com.br.criarcenariotestes.business.autoqa.discovery.scanner;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ProjectScanPolicy {

    private static final int MAX_SCAN_DEPTH = 4;
    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            "target",
            "out",
            "coverage",
            "playwright-report",
            "test-results",
            "blob-report",
            "allure-results",
            "allure-report",
            ".idea",
            ".vscode",
            ".gradle",
            "logs",
            // Diretórios de FERRAMENTA, não do projeto. Sem eles na lista, o
            // scanner indexava .claude/api/auth/*.ts como se fossem componentes
            // do projeto: o plano então concluía que os testes "já existiam" e
            // gerava zero arquivos — e antes disso chegou a espelhar
            // ".claude/api/auth/" como convenção de diretórios, gravando testes
            // dentro da pasta de configuração do Claude Code.
            ".claude",
            ".github",
            ".husky",
            ".circleci",
            ".devcontainer"
    );

    public int maxDepth() {
        return MAX_SCAN_DEPTH;
    }

    public boolean isIgnoredDirectory(String name) {
        return IGNORED_DIRECTORIES.contains(name);
    }

    public boolean isIgnoredFile(String name) {
        String lower = name.toLowerCase();
        return lower.equals(".env")
                || lower.startsWith(".env.")
                || lower.endsWith(".pem")
                || lower.endsWith(".key")
                || lower.endsWith(".crt")
                || lower.endsWith(".cer")
                || lower.endsWith(".p12")
                || lower.endsWith(".pfx")
                || lower.endsWith(".zip")
                || lower.endsWith(".tar")
                || lower.endsWith(".gz")
                || lower.endsWith(".jar")
                || lower.endsWith(".war")
                || lower.endsWith(".class");
    }
}
