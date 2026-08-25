package com.br.criarcenariotestes.business.autoqa.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Compila os arquivos TypeScript gerados, sem emitir saída, e devolve os erros.
 *
 * <p>Existe porque há uma classe inteira de defeito que revisão por IA não pega
 * de forma confiável e um compilador pega de graça. Caso real: o código gerado
 * fez {@code import { playwright } from '@playwright/test'} — que não é export
 * desse pacote, é fixture — e a revisão aprovou. O teste quebraria só na
 * execução, apontando para o lugar errado.
 *
 * <p>Roda dentro do projeto ALVO, não na área de staging: a resolução de
 * módulos precisa enxergar o node_modules dele para tipar @playwright/test. Os
 * arquivos são copiados para um diretório temporário na raiz do alvo e
 * removidos ao final, inclusive em caso de erro.
 *
 * <p>Usa apenas o compilador JÁ INSTALADO no projeto. Nunca invoca npx: baixar
 * pacote em tempo de revisão seria risco de cadeia de suprimentos e deixaria o
 * resultado dependente de rede. Sem compilador, a verificação se declara
 * indisponível em vez de reprovar código correto.
 */
@Component
public class CompilacaoTypeScript {

    private static final Logger log = LoggerFactory.getLogger(CompilacaoTypeScript.class);
    private static final long TIMEOUT_SEGUNDOS = 120;

    /** Erros encontrados, ou vazio. {@code disponivel=false} quando não deu para verificar. */
    public record Resultado(boolean disponivel, String motivoIndisponivel, List<String> erros) {
        public static Resultado indisponivel(String motivo) {
            return new Resultado(false, motivo, List.of());
        }

        public static Resultado de(List<String> erros) {
            return new Resultado(true, null, List.copyOf(erros));
        }
    }

    public Resultado verificar(Path raizDoProjeto, List<GeneratedArtifactReader.ReadArtifact> artefatos) {
        if (raizDoProjeto == null || !Files.isDirectory(raizDoProjeto)) {
            return Resultado.indisponivel("raiz do projeto inacessível");
        }

        List<GeneratedArtifactReader.ReadArtifact> typescript = artefatos.stream()
                .filter(a -> a.relativePath() != null && a.relativePath().endsWith(".ts"))
                .filter(a -> a.content() != null && !a.content().isBlank())
                .toList();
        if (typescript.isEmpty()) {
            return Resultado.indisponivel("nenhum arquivo TypeScript gerado");
        }

        Path compilador = raizDoProjeto.resolve("node_modules/.bin/tsc");
        if (!Files.isExecutable(compilador)) {
            return Resultado.indisponivel(
                    "TypeScript não instalado no projeto (npm i -D typescript) — verificação pulada");
        }
        if (!Files.isRegularFile(raizDoProjeto.resolve("tsconfig.json"))) {
            return Resultado.indisponivel("projeto sem tsconfig.json — verificação pulada");
        }

        Path temporario = raizDoProjeto.resolve(".auto-qa-typecheck-" + UUID.randomUUID());
        try {
            prepararAreaTemporaria(temporario, typescript);
            return Resultado.de(compilar(raizDoProjeto, compilador, temporario));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Verificação de compilação não pôde ser concluída. motivo='{}'", e.getMessage());
            return Resultado.indisponivel("falha ao executar o compilador: " + e.getMessage());
        } finally {
            removerRecursivo(temporario);
        }
    }

    private void prepararAreaTemporaria(Path temporario,
                                        List<GeneratedArtifactReader.ReadArtifact> typescript) throws IOException {
        Files.createDirectories(temporario);
        for (GeneratedArtifactReader.ReadArtifact artefato : typescript) {
            Path destino = temporario.resolve(artefato.relativePath()).normalize();
            if (!destino.startsWith(temporario)) {
                throw new IOException("caminho sai da área temporária: " + artefato.relativePath());
            }
            Files.createDirectories(destino.getParent());
            Files.writeString(destino, artefato.content(), StandardCharsets.UTF_8);
        }
        // Herda as opções do projeto (strict, target, types) e restringe o
        // escopo aos arquivos gerados — compilar o projeto inteiro misturaria
        // erros pré-existentes com os da geração.
        Files.writeString(temporario.resolve("tsconfig.json"),
                "{\n  \"extends\": \"../tsconfig.json\",\n  \"include\": [\"**/*.ts\"]\n}\n",
                StandardCharsets.UTF_8);
    }

    private List<String> compilar(Path raizDoProjeto, Path compilador, Path temporario)
            throws IOException, InterruptedException {
        ProcessBuilder processo = new ProcessBuilder(
                compilador.toString(), "--noEmit", "--pretty", "false",
                "-p", temporario.resolve("tsconfig.json").toString());
        processo.directory(raizDoProjeto.toFile());
        processo.redirectErrorStream(true);

        Process executando = processo.start();
        String saida;
        try (var stream = executando.getInputStream()) {
            saida = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (!executando.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)) {
            executando.destroyForcibly();
            throw new IOException("compilador excedeu " + TIMEOUT_SEGUNDOS + "s");
        }

        List<String> erros = new ArrayList<>();
        for (String linha : saida.split("\\R")) {
            String limpa = linha.trim();
            if (limpa.contains("error TS")) {
                // O caminho vem prefixado pelo diretório temporário, que não
                // existe para quem lê o relatório.
                erros.add(limpa.replace(temporario.getFileName().toString() + "/", ""));
            }
        }
        return erros;
    }

    private void removerRecursivo(Path raiz) {
        if (raiz == null || !Files.exists(raiz)) {
            return;
        }
        try (var caminhos = Files.walk(raiz)) {
            caminhos.sorted(Comparator.reverseOrder()).forEach(caminho -> {
                try {
                    Files.deleteIfExists(caminho);
                } catch (IOException ignorado) {
                    // melhor esforço; diretório temporário identificável pelo nome
                }
            });
        } catch (IOException ignorado) {
            log.warn("Área temporária de type-check não pôde ser removida: {}", raiz);
        }
    }
}
