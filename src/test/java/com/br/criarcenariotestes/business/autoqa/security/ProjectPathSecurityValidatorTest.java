package com.br.criarcenariotestes.business.autoqa.security;

import com.br.criarcenariotestes.business.autoqa.executionapi.config.AutoQaProperties;
import com.br.criarcenariotestes.business.autoqa.executionapi.exception.AutoQaProjectPathNotAllowedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProjectPathSecurityValidator - Testes Unitários (Fase 13.1A)")
class ProjectPathSecurityValidatorTest {

    @TempDir
    Path tempDir;

    private AutoQaProperties properties;
    private Path allowedRoot;
    private Path outsideRoot;

    @BeforeEach
    void setUp() throws IOException {
        properties = new AutoQaProperties();
        allowedRoot = Files.createDirectories(tempDir.resolve("allowed-root"));
        outsideRoot = Files.createDirectories(tempDir.resolve("outside-root"));
    }

    private ProjectPathSecurityValidator validatorWithRoots(String... roots) {
        properties.setAllowedRoots(List.of(roots));
        return new ProjectPathSecurityValidator(properties);
    }

    @Test
    @DisplayName("allowedRoots vazia deve rejeitar qualquer projectPath (fail-closed)")
    void allowedRootsVaziaDeveRejeitarQualquerProjectPath() {
        ProjectPathSecurityValidator validator = validatorWithRoots();

        assertThatThrownBy(() -> validator.validate(allowedRoot))
                .isInstanceOf(AutoQaProjectPathNotAllowedException.class);
    }

    @Test
    @DisplayName("A própria root autorizada deve ser aceita como projectPath")
    void aPropriaRootAutorizadaDeveSerAceita() throws IOException {
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString());

        Path result = validator.validate(allowedRoot);

        assertThat(result).isEqualTo(allowedRoot.toRealPath());
    }

    @Test
    @DisplayName("Subdiretório de uma root autorizada deve ser aceito")
    void subdiretorioDeRootAutorizadaDeveSerAceito() throws IOException {
        Path subdir = Files.createDirectories(allowedRoot.resolve("meu-projeto"));
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString());

        Path result = validator.validate(subdir);

        assertThat(result).isEqualTo(subdir.toRealPath());
    }

    @Test
    @DisplayName("Path fora de todas as roots autorizadas deve ser rejeitado")
    void pathForaDeTodasAsRootsDeveSerRejeitado() {
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString());

        assertThatThrownBy(() -> validator.validate(outsideRoot))
                .isInstanceOf(AutoQaProjectPathNotAllowedException.class);
    }

    @Test
    @DisplayName("Path válido em uma dentre múltiplas roots autorizadas deve ser aceito")
    void pathValidoEmUmaDeMultiplasRootsDeveSerAceito() throws IOException {
        Path secondRoot = Files.createDirectories(tempDir.resolve("second-root"));
        Path insideSecond = Files.createDirectories(secondRoot.resolve("projeto"));
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString(), secondRoot.toString());

        Path result = validator.validate(insideSecond);

        assertThat(result).isEqualTo(insideSecond.toRealPath());
    }

    @Test
    @DisplayName("Não deve considerar path como autorizado apenas por prefixo textual parecido")
    void naoDeveConsiderarPrefixoTextualParecidoComoAutorizado() throws IOException {
        Path lookalike = Files.createDirectories(tempDir.resolve("allowed-root-malicioso"));
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString());

        assertThatThrownBy(() -> validator.validate(lookalike))
                .isInstanceOf(AutoQaProjectPathNotAllowedException.class);
    }

    @Test
    @DisplayName("Traversal que permanece dentro da root autorizada deve ser aceito")
    void traversalQuePermaneceDentroDaRootDeveSerAceito() throws IOException {
        Files.createDirectories(allowedRoot.resolve("foo"));
        Path bar = Files.createDirectories(allowedRoot.resolve("bar"));
        Path traversal = allowedRoot.resolve("foo").resolve("..").resolve("bar");
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString());

        Path result = validator.validate(traversal);

        assertThat(result).isEqualTo(bar.toRealPath());
    }

    @Test
    @DisplayName("Traversal que escapa da root autorizada deve ser rejeitado")
    void traversalQueEscapaDaRootDeveSerRejeitado() {
        Path traversal = allowedRoot.resolve("..").resolve("outside-root");
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString());

        assertThatThrownBy(() -> validator.validate(traversal))
                .isInstanceOf(AutoQaProjectPathNotAllowedException.class);
    }

    @Test
    @DisplayName("Symlink dentro da root autorizada apontando para fora deve ser rejeitado")
    void symlinkApontandoParaForaDeveSerRejeitado() throws IOException {
        Path escape = allowedRoot.resolve("external");
        Files.createSymbolicLink(escape, outsideRoot);
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString());

        assertThatThrownBy(() -> validator.validate(escape))
                .isInstanceOf(AutoQaProjectPathNotAllowedException.class);
    }

    @Test
    @DisplayName("Symlink dentro da root autorizada apontando para dentro dela mesma deve ser aceito")
    void symlinkApontandoParaDentroDeveSerAceito() throws IOException {
        Path real = Files.createDirectories(allowedRoot.resolve("projeto-real"));
        Path link = allowedRoot.resolve("projeto-via-link");
        Files.createSymbolicLink(link, real);
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString());

        Path result = validator.validate(link);

        assertThat(result).isEqualTo(real.toRealPath());
    }

    @Test
    @DisplayName("A própria root, quando é um symlink apontando para fora, deve ser rejeitada")
    void rootQueESymlinkApontandoParaForaDeveSerRejeitada() throws IOException {
        Path linkRoot = tempDir.resolve("link-root");
        Files.createSymbolicLink(linkRoot, outsideRoot);
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString());

        assertThatThrownBy(() -> validator.validate(linkRoot))
                .isInstanceOf(AutoQaProjectPathNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve rejeitar projectPath inexistente")
    void deveRejeitarProjectPathInexistente() {
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString());
        Path missing = allowedRoot.resolve("nao-existe");

        assertThatThrownBy(() -> validator.validate(missing))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar projectPath que seja um arquivo, não diretório")
    void deveRejeitarProjectPathQueSejaArquivo() throws IOException {
        Path file = Files.writeString(allowedRoot.resolve("arquivo.txt"), "conteudo");
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString());

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve rejeitar projectPath nulo")
    void deveRejeitarProjectPathNulo() {
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString());

        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Deve rejeitar projectPath em branco")
    void deveRejeitarProjectPathEmBranco() {
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString());

        assertThatThrownBy(() -> validator.validate(Path.of("   ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Root configurada inexistente deve ser ignorada, sem ampliar permissões")
    void rootConfiguradaInexistenteDeveSerIgnorada() {
        String rootInexistente = tempDir.resolve("nao-existe-nunca").toString();
        ProjectPathSecurityValidator validator = validatorWithRoots(rootInexistente);

        assertThatThrownBy(() -> validator.validate(allowedRoot))
                .isInstanceOf(AutoQaProjectPathNotAllowedException.class);
    }

    @Test
    @DisplayName("Root configurada apontando para um arquivo (não diretório) deve ser ignorada")
    void rootConfiguradaApontandoParaArquivoDeveSerIgnorada() throws IOException {
        Path fileAsRoot = Files.writeString(tempDir.resolve("nao-e-diretorio.txt"), "conteudo");
        ProjectPathSecurityValidator validator = validatorWithRoots(fileAsRoot.toString());

        assertThatThrownBy(() -> validator.validate(allowedRoot))
                .isInstanceOf(AutoQaProjectPathNotAllowedException.class);
    }

    @Test
    @DisplayName("Deve normalizar segmentos redundantes (// e .) antes de comparar com a root")
    void deveNormalizarSegmentosRedundantes() throws IOException {
        Path subdir = Files.createDirectories(allowedRoot.resolve("projeto"));
        Path withRedundantSegments = Path.of(allowedRoot + "/./projeto");
        ProjectPathSecurityValidator validator = validatorWithRoots(allowedRoot.toString());

        Path result = validator.validate(withRedundantSegments);

        assertThat(result).isEqualTo(subdir.toRealPath());
    }
}
