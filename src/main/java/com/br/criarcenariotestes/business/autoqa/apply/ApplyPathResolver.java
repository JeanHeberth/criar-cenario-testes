package com.br.criarcenariotestes.business.autoqa.apply;

import com.br.criarcenariotestes.business.autoqa.apply.exception.ApplyConflictException;
import com.br.criarcenariotestes.business.autoqa.model.apply.ApplyConflict;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Única classe autorizada a resolver o caminho físico de um relativePath
 * dentro de uma raiz confiável (projeto real ou área de backup). Nunca
 * modifica nada em disco — apenas resolve e valida.
 */
@Component
public class ApplyPathResolver {

    private static final Pattern PATH_TRAVERSAL = Pattern.compile("\\.\\./");
    private static final Pattern ABSOLUTE_UNIX = Pattern.compile("^/");
    private static final Pattern ABSOLUTE_WINDOWS = Pattern.compile("(?i)^[A-Za-z]:\\\\");
    private static final Pattern ABSOLUTE_UNC = Pattern.compile("^\\\\\\\\");
    private static final Pattern FILE_URI = Pattern.compile("(?i)^file://");

    /**
     * Diretórios que pertencem a FERRAMENTAS, não ao projeto. Um plano jamais
     * deve gravar código de teste dentro deles.
     *
     * <p>Observado em produção: com {@code tests/} vazio, o scanner enxergava
     * apenas {@code .claude/} e {@code .github/} e "detectou" {@code .claude}
     * como o padrão de diretórios do projeto. O planner seguiu esse padrão
     * fielmente e o apply gravou login.spec.ts dentro da pasta de configuração
     * do Claude Code. A exclusão no scanner corrige a origem; esta barreira
     * garante que nenhum plano futuro consiga escrever ali de novo.
     *
     * <p>Vale para SEGMENTOS DE DIRETÓRIO apenas — arquivos ocultos legítimos
     * na raiz (.env.example, .gitignore) continuam permitidos.
     */
    private static final Set<String> DIRETORIOS_DE_FERRAMENTA = Set.of(
            ".claude", ".git", ".github", ".idea", ".vscode", ".gradle",
            ".husky", ".circleci", ".devcontainer", ".auto-qa", "node_modules");

    public Path resolve(Path root, String relativePath) {
        Objects.requireNonNull(root, "root must not be null");
        if (relativePath == null || relativePath.isBlank()) {
            throw new ApplyConflictException(relativePath, ApplyConflict.PATH_SECURITY_VIOLATION,
                    "relativePath must not be blank");
        }
        if (PATH_TRAVERSAL.matcher(relativePath).find()) {
            throw new ApplyConflictException(relativePath, ApplyConflict.PATH_SECURITY_VIOLATION,
                    "Path traversal detectado: " + relativePath);
        }
        if (ABSOLUTE_UNIX.matcher(relativePath).find()
                || ABSOLUTE_WINDOWS.matcher(relativePath).find()
                || ABSOLUTE_UNC.matcher(relativePath).find()
                || FILE_URI.matcher(relativePath).find()) {
            throw new ApplyConflictException(relativePath, ApplyConflict.PATH_SECURITY_VIOLATION,
                    "Caminho absoluto não permitido: " + relativePath);
        }

        rejeitarDiretorioDeFerramenta(relativePath);

        Path normalizedRoot = root.normalize();
        Path resolved = normalizedRoot.resolve(relativePath).normalize();

        if (!resolved.equals(normalizedRoot) && !resolved.startsWith(normalizedRoot)) {
            throw new ApplyConflictException(relativePath, ApplyConflict.PATH_SECURITY_VIOLATION,
                    "Caminho resolvido sai da raiz: " + relativePath);
        }

        rejectSymlinkSegments(normalizedRoot, resolved, relativePath);

        return resolved;
    }

    private void rejeitarDiretorioDeFerramenta(String relativePath) {
        String[] segmentos = relativePath.replace('\\', '/').split("/");
        // O último segmento é o nome do arquivo: não entra na checagem.
        for (int i = 0; i < segmentos.length - 1; i++) {
            if (DIRETORIOS_DE_FERRAMENTA.contains(segmentos[i].toLowerCase())) {
                throw new ApplyConflictException(relativePath, ApplyConflict.PATH_SECURITY_VIOLATION,
                        "Caminho aponta para diretório de ferramenta, não do projeto: " + segmentos[i]);
            }
        }
    }

    private void rejectSymlinkSegments(Path root, Path resolved, String relativePath) {
        Path current = resolved;
        while (current != null && (current.equals(root) || current.startsWith(root))) {
            if (Files.isSymbolicLink(current)) {
                throw new ApplyConflictException(relativePath, ApplyConflict.PATH_SECURITY_VIOLATION,
                        "Symlink detectado no caminho: " + current);
            }
            if (current.equals(root)) {
                break;
            }
            current = current.getParent();
        }
    }
}
