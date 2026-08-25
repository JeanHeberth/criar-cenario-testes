package com.br.criarcenariotestes.business.autoqa.generation;

import com.br.criarcenariotestes.business.autoqa.generation.exception.GenerationWriteException;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileStatus;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetentativaDeGeracaoTest {

    private final GeneratedFileWriter writer = new GeneratedFileWriter(new GeneratedPathResolver());

    private GeneratedFile arquivo(String caminho, String conteudo) {
        return new GeneratedFile(caminho, GeneratedFileOperation.CREATE, PlanComponentType.API_CLIENT,
                conteudo, "UTF-8", null, GeneratedFileStatus.GENERATED, false, List.of(), List.of(), List.of());
    }

    @Test
    void writerDeveRecusarSobrescritaDentroDaMesmaRodada(@TempDir Path base) {
        // Esta proteção é deliberada e continua valendo: dois arquivos do mesmo
        // lote no mesmo caminho é defeito de geração, não retentativa.
        UUID id = UUID.randomUUID();
        writer.write(base, id, arquivo("tests/api/auth/apiClient.ts", "v1"));

        assertThatThrownBy(() -> writer.write(base, id, arquivo("tests/api/auth/apiClient.ts", "v2")))
                .isInstanceOf(GenerationWriteException.class)
                .hasMessageContaining("já existe");
    }

    @Test
    void limpezaDaAreaDeveLiberarNovaTentativa(@TempDir Path base) throws Exception {
        // Regressão do beco sem saída observado em produção: a geração concluiu,
        // o review falhou depois, e todo CONTINUE seguinte morria com
        // "Arquivo já existe na área gerada" — o retry nunca podia dar certo.
        UUID id = UUID.randomUUID();
        writer.write(base, id, arquivo("tests/api/auth/apiClient.ts", "tentativa 1"));

        Path raiz = base.resolve(id.toString()).resolve("files");
        assertThat(Files.exists(raiz.resolve("tests/api/auth/apiClient.ts"))).isTrue();

        limparComoOServicoFaz(raiz);

        assertThat(Files.exists(raiz.resolve("tests/api/auth/apiClient.ts"))).isFalse();
        writer.write(base, id, arquivo("tests/api/auth/apiClient.ts", "tentativa 2"));
        assertThat(Files.readString(raiz.resolve("tests/api/auth/apiClient.ts"))).isEqualTo("tentativa 2");
    }

    @Test
    void limpezaNaoDeveTocarEmOutraExecucao(@TempDir Path base) throws Exception {
        UUID alvo = UUID.randomUUID();
        UUID vizinha = UUID.randomUUID();
        writer.write(base, alvo, arquivo("tests/a.ts", "alvo"));
        writer.write(base, vizinha, arquivo("tests/a.ts", "vizinha"));

        limparComoOServicoFaz(base.resolve(alvo.toString()).resolve("files"));

        assertThat(Files.readString(base.resolve(vizinha.toString()).resolve("files").resolve("tests/a.ts")))
                .as("a limpeza é escopada pelo executionId — execução vizinha é intocada")
                .isEqualTo("vizinha");
    }

    /** Espelha limparAreaGerada do GenerationService. */
    private void limparComoOServicoFaz(Path raiz) throws Exception {
        if (!Files.isDirectory(raiz)) return;
        try (var caminhos = Files.walk(raiz)) {
            caminhos.filter(c -> c.startsWith(raiz) && !c.equals(raiz))
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(c -> {
                        try { Files.deleteIfExists(c); } catch (java.io.IOException ignorado) { }
                    });
        }
    }

    @Test
    void regeracaoParcialDevePreservarOsBytesDosArquivosNaoRegerados(@TempDir Path base) throws Exception {
        // Regressão de um conflito entre duas correções minhas: a limpeza da
        // área apagava TUDO antes da tentativa, enquanto o resultado carregava
        // o registro dos arquivos reaproveitados. Sobrava o registro sem os
        // bytes, e a revisão falhava com "Arquivo gerado não encontrado".
        UUID id = UUID.randomUUID();
        writer.write(base, id, arquivo("tests/cliente.ts", "cliente v1"));
        writer.write(base, id, arquivo("tests/spec.ts", "spec v1"));

        Path raiz = base.resolve(id.toString()).resolve("files");
        limparApenas(raiz, List.of("tests/spec.ts"));

        assertThat(Files.readString(raiz.resolve("tests/cliente.ts")))
                .as("arquivo reaproveitado mantém os bytes em disco")
                .isEqualTo("cliente v1");
        assertThat(Files.exists(raiz.resolve("tests/spec.ts")))
                .as("arquivo que será refeito sai do caminho do writer")
                .isFalse();
    }

    /** Espelha limparAreaGerada com lista de arquivos a regerar. */
    private void limparApenas(Path raiz, List<String> arquivos) {
        for (String relativo : arquivos) {
            try { Files.deleteIfExists(raiz.resolve(relativo).normalize()); } catch (java.io.IOException ignorado) { }
        }
    }
}
