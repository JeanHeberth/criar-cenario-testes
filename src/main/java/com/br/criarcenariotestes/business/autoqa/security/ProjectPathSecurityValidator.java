package com.br.criarcenariotestes.business.autoqa.security;

import com.br.criarcenariotestes.business.autoqa.executionapi.config.AutoQaProperties;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaProjectPathNotAllowedException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fonte única de verdade para autorização de projectPath (Fase 13.1A).
 * Usada por AutoQaExecutionOrchestrator.create() (rejeição antecipada, antes
 * de persistir) e por ProjectDiscoveryService (validação autoritativa, antes
 * de qualquer leitura real de filesystem) — nunca deve ser reimplementada
 * em nenhum outro ponto do pipeline (Apply/Execute herdam a proteção porque
 * ambos consomem o Path já validado por este componente via
 * ProjectDiscoveryResult).
 *
 * Política fail-closed: auto-qa.allowed-roots vazia (ou contendo somente
 * entradas inválidas) significa que NENHUM projectPath é autorizado — não
 * há fallback implícito para o diretório atual, home do usuário ou /tmp.
 *
 * A comparação contra as roots autorizadas é sempre feita após resolver o
 * caminho real (Path.toRealPath(), que segue symlinks), nunca por prefixo
 * de String — assim um symlink dentro de uma root autorizada apontando para
 * fora dela é rejeitado (o destino real é o que importa, não a aparência
 * textual do caminho).
 */
@Component
public class ProjectPathSecurityValidator {

    private final AutoQaProperties properties;

    public ProjectPathSecurityValidator(AutoQaProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * Resolve, valida e autoriza projectPath. Retorna o Path real
     * (symlinks resolvidos) a ser usado como raiz confiável pelo restante
     * do pipeline. Lança IllegalArgumentException para problemas de forma
     * (nulo, vazio, inexistente, não-diretório, não-legível — mesmas
     * categorias já existentes antes desta subfase) e
     * AutoQaProjectPathNotAllowedException quando o path é válido mas está
     * fora de qualquer raiz autorizada.
     */
    public Path validate(Path projectPath) {
        Objects.requireNonNull(projectPath, "projectPath must not be null");
        if (projectPath.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("projectPath must not be blank");
        }

        Path normalized = projectPath.toAbsolutePath().normalize();
        Path real = toRealPathOrNull(normalized);
        if (real == null) {
            throw new IllegalArgumentException("projectPath does not exist");
        }
        if (!Files.isDirectory(real)) {
            throw new IllegalArgumentException("projectPath must be a directory");
        }
        if (!Files.isReadable(real)) {
            throw new IllegalArgumentException("projectPath must be readable");
        }

        List<Path> authorizedRoots = resolveAuthorizedRoots();
        boolean allowed = authorizedRoots.stream()
                .anyMatch(root -> real.equals(root) || real.startsWith(root));
        if (!allowed) {
            throw new AutoQaProjectPathNotAllowedException("Project path is not inside an allowed root.");
        }

        return real;
    }

    /**
     * As raízes autorizadas já resolvidas, para quem precisa navegá-las (o
     * seletor de pastas da tela do Auto QA) em vez de validar um caminho
     * pronto. Exposta aqui de propósito: a resolução continua acontecendo num
     * lugar só, e o navegador de pastas não pode reimplementar a política —
     * fazer isso seria criar um caminho paralelo que ignora fail-closed e
     * resolução de symlink.
     */
    public List<Path> listarRaizesAutorizadas() {
        return resolveAuthorizedRoots();
    }

    /**
     * Cada root configurada é resolvida da mesma forma que projectPath
     * (absoluto, normalizado, symlink resolvido). Uma root que não existe,
     * não é diretório ou não pode ser resolvida é simplesmente ignorada —
     * uma configuração inválida nunca pode ampliar permissões (nunca vira
     * "permitir tudo"). Lista vazia (default) resulta em nenhuma root
     * autorizada, ou seja, fail-closed.
     */
    private List<Path> resolveAuthorizedRoots() {
        List<Path> roots = new ArrayList<>();
        for (String candidate : properties.getAllowedRoots()) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            try {
                Path candidateNormalized = Path.of(candidate).toAbsolutePath().normalize();
                Path candidateReal = candidateNormalized.toRealPath();
                if (Files.isDirectory(candidateReal)) {
                    roots.add(candidateReal);
                }
            } catch (IOException | InvalidPathException ex) {
                // root configurada inexistente/irresolvível: ignorada, nunca amplia permissão.
            }
        }
        return roots;
    }

    private Path toRealPathOrNull(Path normalized) {
        try {
            return normalized.toRealPath();
        } catch (IOException ex) {
            return null;
        }
    }
}
