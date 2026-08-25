package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompilacaoTypeScriptTest {

    private final CompilacaoTypeScript verificador = new CompilacaoTypeScript();

    private GeneratedArtifactReader.ReadArtifact artefato(String caminho, String conteudo) {
        return new GeneratedArtifactReader.ReadArtifact(caminho, GeneratedFileOperation.CREATE,
                PlanComponentType.TEST, conteudo, "hash", true);
    }

    @Test
    void deveSeDeclararIndisponivelSemCompiladorNoProjeto(@TempDir Path projeto) throws Exception {
        // A falta da ferramenta no projeto alvo não diz nada sobre a qualidade
        // do código gerado — reprovar aqui seria punir o inocente.
        Files.writeString(projeto.resolve("tsconfig.json"), "{}");

        var resultado = verificador.verificar(projeto, List.of(artefato("t.ts", "const x = 1;")));

        assertThat(resultado.disponivel()).isFalse();
        assertThat(resultado.motivoIndisponivel()).contains("TypeScript não instalado");
        assertThat(resultado.erros()).isEmpty();
    }

    @Test
    void deveSeDeclararIndisponivelQuandoNaoHaTypeScriptGerado(@TempDir Path projeto) {
        var resultado = verificador.verificar(projeto, List.of(artefato("config.json", "{}")));

        assertThat(resultado.disponivel()).isFalse();
        assertThat(resultado.motivoIndisponivel()).contains("nenhum arquivo TypeScript");
    }

    @Test
    void deveSeDeclararIndisponivelComRaizInacessivel() {
        var resultado = verificador.verificar(Path.of("/caminho/que/nao/existe"),
                List.of(artefato("t.ts", "const x = 1;")));

        assertThat(resultado.disponivel()).isFalse();
        assertThat(resultado.motivoIndisponivel()).contains("raiz do projeto inacessível");
    }

    @Test
    void naoDeveDeixarAreaTemporariaParaTras(@TempDir Path projeto) throws Exception {
        // O diretório temporário fica na raiz do projeto do usuário para que a
        // resolução de módulos enxergue o node_modules dele. Se ficasse para
        // trás, poluiria o projeto e seria indexado na execução seguinte.
        Files.writeString(projeto.resolve("tsconfig.json"), "{}");

        verificador.verificar(projeto, List.of(artefato("tests/t.ts", "const x = 1;")));

        try (var conteudo = Files.list(projeto)) {
            assertThat(conteudo.map(p -> p.getFileName().toString()))
                    .as("nenhum resíduo .auto-qa-typecheck-*")
                    .noneMatch(nome -> nome.startsWith(".auto-qa-typecheck"));
        }
    }
}
