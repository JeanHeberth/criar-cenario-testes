package com.br.criarcenariotestes.business.validation;

import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FASE15-BUG-002: valida estruturalmente a resposta bruta da IA e os
 * cenários extraídos ANTES de seguirem para o Redundancy Reviewer/persistência.
 * Reproduz o defeito real observado em produção: a IA às vezes responde com
 * um "Plano de Geração" (formato de arquivos) em vez de cenários de teste.
 */
@DisplayName("GeneratedScenariosValidator - Testes Unitários")
class GeneratedScenariosValidatorTest {

    private final GeneratedScenariosValidator validator = new GeneratedScenariosValidator();

    @Test
    @DisplayName("Deve rejeitar resposta em formato de 'Plano de Geração' (defeito real de produção)")
    void deveRejeitarRespostaEmFormatoDePlanoDeGeracao() {
        String planoDeGeracao = """
                📋 Plano de Geração
                - Pasta base: `login_bloqueio_2fa_tests/`
                - Arquivos a criar:
                  - `CENARIOS_DE_TESTE.md` – Todos os cenários detalhados
                """;

        CenarioItem itemGarbled = new CenarioItem();
        itemGarbled.setNome("Plano de Geração");

        GeneratedScenariosValidator.ValidationResult resultado =
                validator.validarGeracao(planoDeGeracao, List.of(itemGarbled));

        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.motivo()).isNotBlank();
    }

    @Test
    @DisplayName("Deve rejeitar resposta nula")
    void deveRejeitarRespostaNula() {
        GeneratedScenariosValidator.ValidationResult resultado = validator.validarGeracao(null, null);

        assertThat(resultado.valido()).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar resposta vazia/em branco")
    void deveRejeitarRespostaEmBranco() {
        GeneratedScenariosValidator.ValidationResult resultado = validator.validarGeracao("   ", List.of());

        assertThat(resultado.valido()).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar texto arbitrário que não é um cenário de teste")
    void deveRejeitarTextoArbitrarioSemCenarios() {
        String textoSemCenarios = "Vou analisar sua solicitação e criar os testes posteriormente.";

        GeneratedScenariosValidator.ValidationResult resultado =
                validator.validarGeracao(textoSemCenarios, List.of());

        assertThat(resultado.valido()).isFalse();
    }

    @Test
    @DisplayName("Deve rejeitar cenário com nome mas sem nenhum conteúdo estrutural mínimo (template vazio)")
    void deveRejeitarCenarioComTemplateVazio() {
        CenarioItem itemVazio = new CenarioItem();
        itemVazio.setNome("Cenário 1");

        GeneratedScenariosValidator.ValidationResult resultado =
                validator.validarGeracao("---\nNome: Cenário 1\n---", List.of(itemVazio));

        assertThat(resultado.valido()).isFalse();
    }

    @Test
    @DisplayName("Deve aceitar resposta com cenários de teste válidos, completos e em formato BDD")
    void deveAceitarRespostaComCenariosValidos() {
        String respostaValida = """
                ---
                Nome: Login com credenciais válidas
                Objetivo: Validar login bem-sucedido
                Passos:
                Dado que o usuário está na tela de login
                Quando ele informa credenciais válidas
                Então o login é realizado com sucesso
                Resultado esperado: Login realizado
                ---
                """;

        CenarioItem item = new CenarioItem();
        item.setNome("Login com credenciais válidas");
        item.setObjetivo("Validar login bem-sucedido");
        item.setScriptTeste("Dado que o usuário está na tela de login\nQuando ele informa credenciais válidas\nEntão o login é realizado com sucesso");
        item.setResultadoEsperado("Login realizado");

        GeneratedScenariosValidator.ValidationResult resultado =
                validator.validarGeracao(respostaValida, List.of(item));

        assertThat(resultado.valido()).isTrue();
        assertThat(resultado.motivo()).isNull();
    }

    @Test
    @DisplayName("validarRespostaBruta: deve rejeitar plano de arquivos independente dos cenários extraídos")
    void validarRespostaBrutaDeveRejeitarPlanoDeArquivos() {
        String plano = "📋 Plano de Geração - Pasta base: `x/` - Arquivos a criar: - `a.md`";

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarRespostaBruta(plano);

        assertThat(resultado.valido()).isFalse();
    }

    @Test
    @DisplayName("validarRespostaBruta: deve aceitar texto que não é um plano de arquivos")
    void validarRespostaBrutaDeveAceitarTextoNormal() {
        GeneratedScenariosValidator.ValidationResult resultado =
                validator.validarRespostaBruta("---\nNome: CT001\n---");

        assertThat(resultado.valido()).isTrue();
    }

    // ===== FASE15-BUG-003: validação estrutural de BDD/Gherkin =====

    @Test
    @DisplayName("validarEstruturaBdd: Dado/Quando/Então simples deve ser VALID")
    void validarEstruturaBddDeveAceitarDadoQuandoEntaoSimples() {
        String passos = """
                Dado que o usuário está na tela de login
                Quando ele informa credenciais válidas
                Então o login é realizado com sucesso
                """;

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarEstruturaBdd(passos);

        assertThat(resultado.valido()).isTrue();
    }

    @Test
    @DisplayName("validarEstruturaBdd: Dado/E/Quando/E/Então/E deve ser VALID")
    void validarEstruturaBddDeveAceitarComPalavraEEntreOsPassos() {
        String passos = """
                Dado que o usuário está cadastrado e ativo
                E possui credenciais válidas
                Quando ele acessa a tela de login e submete o formulário
                E informa o código 2FA correto
                Então o login é concluído com sucesso
                E o usuário é redirecionado para a página inicial
                """;

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarEstruturaBdd(passos);

        assertThat(resultado.valido()).isTrue();
    }

    @Test
    @DisplayName("validarEstruturaBdd: passos numerados devem ser INVALID para o contrato atual")
    void validarEstruturaBddDeveRejeitarPassosNumerados() {
        String passos = """
                1. Acessar tela de login
                2. Informar credenciais válidas
                3. Clicar em Login
                """;

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarEstruturaBdd(passos);

        assertThat(resultado.valido()).isFalse();
    }

    @Test
    @DisplayName("validarEstruturaBdd: palavras BDD no meio da frase não contam como keyword")
    void validarEstruturaBddNaoDeveConsiderarPalavrasNoMeioDaFrase() {
        String passos = "O sistema deve bloquear quando houver três erros e então informar o usuário.";

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarEstruturaBdd(passos);

        assertThat(resultado.valido()).isFalse();
    }

    @Test
    @DisplayName("validarEstruturaBdd: sem Dado deve ser INVALID")
    void validarEstruturaBddDeveRejeitarSemDado() {
        String passos = """
                Quando ele informa credenciais válidas
                Então o login é realizado com sucesso
                """;

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarEstruturaBdd(passos);

        assertThat(resultado.valido()).isFalse();
    }

    @Test
    @DisplayName("validarEstruturaBdd: sem Quando deve ser INVALID")
    void validarEstruturaBddDeveRejeitarSemQuando() {
        String passos = """
                Dado que o usuário está na tela de login
                Então o login é realizado com sucesso
                """;

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarEstruturaBdd(passos);

        assertThat(resultado.valido()).isFalse();
    }

    @Test
    @DisplayName("validarEstruturaBdd: sem Então deve ser INVALID")
    void validarEstruturaBddDeveRejeitarSemEntao() {
        String passos = """
                Dado que o usuário está na tela de login
                Quando ele informa credenciais válidas
                """;

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarEstruturaBdd(passos);

        assertThat(resultado.valido()).isFalse();
    }

    @Test
    @DisplayName("validarGeracao: deve rejeitar cenário com passos numerados mesmo com todos os outros campos válidos")
    void validarGeracaoDeveRejeitarCenarioComPassosNumerados() {
        CenarioItem item = new CenarioItem();
        item.setNome("Login com credenciais válidas");
        item.setObjetivo("Validar login bem-sucedido");
        item.setScriptTeste("1. Acessar tela de login\n2. Informar credenciais válidas");
        item.setResultadoEsperado("Login realizado");

        String respostaBruta = "---\nNome: Login com credenciais válidas\nPassos:\n1. Acessar tela\n2. Login\n---";

        GeneratedScenariosValidator.ValidationResult resultado =
                validator.validarGeracao(respostaBruta, List.of(item));

        assertThat(resultado.valido()).isFalse();
    }
}
