package com.br.criarcenariotestes.business.autoqa.navegacao;

import com.br.criarcenariotestes.business.autoqa.security.ProjectPathSecurityValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Navegação pelas pastas que o Auto QA tem permissão de usar, para o seletor
 * da tela — o usuário escolhe o diretório do projeto em vez de digitar o
 * caminho inteiro à mão.
 *
 * Precisa existir do lado do servidor porque o navegador não entrega caminho
 * absoluto: nem input[webkitdirectory] nem showDirectoryPicker() expõem onde a
 * pasta fica no disco, só o nome dela. Quem consegue enumerar o filesystem é
 * quem roda nele.
 *
 * Toda autorização passa por ProjectPathSecurityValidator, nunca por lógica
 * própria daqui: reimplementar a política criaria um caminho paralelo capaz de
 * ignorar o fail-closed e a resolução de symlink que ele garante.
 */
@Service
@RequiredArgsConstructor
public class NavegacaoPastasService {

    private static final Logger log = LoggerFactory.getLogger(NavegacaoPastasService.class);

    private final ProjectPathSecurityValidator projectPathSecurityValidator;

    /**
     * @param caminho pasta a listar; vazio/nulo devolve as raízes autorizadas.
     */
    public NavegacaoPastasResponse listar(String caminho) {
        List<Path> raizes = projectPathSecurityValidator.listarRaizesAutorizadas();

        if (caminho == null || caminho.isBlank()) {
            return listarRaizes(raizes);
        }

        // validate() resolve symlinks e rejeita qualquer coisa fora das raízes
        // autorizadas, lançando as mesmas exceções já tratadas pelo handler do
        // Auto QA. Só depois de passar por ele tocamos no disco.
        Path real = projectPathSecurityValidator.validate(Path.of(caminho));

        return new NavegacaoPastasResponse(
                real.toString(),
                resolverCaminhoPai(real, raizes),
                true,
                listarSubpastas(real)
        );
    }

    /**
     * O primeiro nível não é um diretório de verdade, e sim o conjunto de
     * raízes configuradas. Com auto-qa.allowed-roots vazia (o default), volta
     * vazio — fail-closed: sem configuração explícita, nada é navegável.
     */
    private NavegacaoPastasResponse listarRaizes(List<Path> raizes) {
        List<NavegacaoPastasResponse.PastaNavegavel> pastas = raizes.stream()
                .map(raiz -> new NavegacaoPastasResponse.PastaNavegavel(
                        raiz.getFileName() == null ? raiz.toString() : raiz.getFileName().toString(),
                        raiz.toString()))
                .sorted(Comparator.comparing(NavegacaoPastasResponse.PastaNavegavel::nome,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (pastas.isEmpty()) {
            log.warn("Seletor de pastas sem nenhuma raiz navegável - auto-qa.allowed-roots não está configurada.");
        }

        // As raízes não são selecionáveis como "caminho atual" porque aqui não
        // se está dentro de nenhuma pasta ainda.
        return new NavegacaoPastasResponse(null, null, false, pastas);
    }

    private List<NavegacaoPastasResponse.PastaNavegavel> listarSubpastas(Path pasta) {
        List<NavegacaoPastasResponse.PastaNavegavel> subpastas = new ArrayList<>();

        try (Stream<Path> filhos = Files.list(pasta)) {
            filhos.filter(Files::isDirectory)
                    .filter(Files::isReadable)
                    // Pastas ocultas (.git, .idea, node_modules escondido) só
                    // poluem a escolha do diretório do projeto.
                    .filter(p -> p.getFileName() != null && !p.getFileName().toString().startsWith("."))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .forEach(p -> subpastas.add(new NavegacaoPastasResponse.PastaNavegavel(
                            p.getFileName().toString(), p.toString())));
        } catch (IOException e) {
            // Pasta ilegível não é erro de requisição: mostra vazia e deixa o
            // usuário voltar, em vez de derrubar o seletor inteiro.
            log.warn("Falha ao listar subpastas de '{}': {}", pasta, e.getMessage());
        }

        return subpastas;
    }

    /**
     * Só permite subir enquanto o pai continuar dentro de alguma raiz
     * autorizada; numa raiz, devolve null para o "voltar" levar de volta à
     * lista de raízes em vez de escapar da área permitida.
     */
    private String resolverCaminhoPai(Path atual, List<Path> raizes) {
        boolean estaNumaRaiz = raizes.stream().anyMatch(atual::equals);
        if (estaNumaRaiz) {
            return null;
        }

        Path pai = atual.getParent();
        if (pai == null) {
            return null;
        }

        boolean paiAutorizado = raizes.stream().anyMatch(raiz -> pai.equals(raiz) || pai.startsWith(raiz));
        return paiAutorizado ? pai.toString() : null;
    }
}
