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
    @DisplayName("validarGeracao: deve rejeitar geração com cenário estruturalmente BDD válido mas com evidência inconsistente (EXPLORATORY+APPROVED)")
    void validarGeracaoDeveRejeitarCenarioComEvidenciaInconsistente() {
        String resposta = """
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
        item.setStatus("APPROVED");
        item.setEvidenceType("EXPLORATORY");

        GeneratedScenariosValidator.ValidationResult resultado =
                validator.validarGeracao(resposta, List.of(item));

        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.motivo()).isNotBlank();
    }

    @Test
    @DisplayName("validarGeracao: deve aceitar geração com cenário BDD válido e evidência consistente (DOCUMENTED com fonte preenchida)")
    void validarGeracaoDeveAceitarCenarioComEvidenciaConsistente() {
        String resposta = """
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
        item.setStatus("APPROVED");
        item.setEvidenceType("DOCUMENTED");
        item.setEvidenceSources("RN-A-01");

        GeneratedScenariosValidator.ValidationResult resultado =
                validator.validarGeracao(resposta, List.of(item));

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

    // ===== FASE15-BUG-003A: fronteira ") Quando" e validação final pós-Formatter =====

    @Test
    @DisplayName("validarEstruturaBdd: 'Quando' após fechamento de parêntese deve ser reconhecido como novo passo (caso real da Sessão 5)")
    void validarEstruturaBddDeveReconhecerQuandoAposParenteseFechado() {
        // Reproduz o padrão real: "Quando" ficou colado ao parêntese do Dado
        // (sem quebra de linha), mas "Então" continua isolado em sua própria
        // linha — exatamente o padrão observado no cenário 12 da Sessão 5.
        String passos = "Dado que o usuário realiza tentativas de login e validação 2FA "
                + "(incluindo tentativas inválidas) Quando ocorre qualquer erro ou rejeição de login\n"
                + "Então os logs do sistema não contêm informações sensíveis";

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarEstruturaBdd(passos);

        assertThat(resultado.valido()).isTrue();
    }

    @Test
    @DisplayName("validarEstruturaBdd: 'quando' minúsculo colado a parêntese (ex.: chamada de função) não deve ser tratado como keyword BDD")
    void validarEstruturaBddNaoDeveReconhecerQuandoMinusculoColadoAParentese() {
        String passos = "Dado que o sistema chama a função quando(condicao) e retorna o valor esperado";

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarEstruturaBdd(passos);

        // Sem "Quando" (maiúsculo, delimitado) e sem "Então" -> continua INVALID,
        // mas o importante aqui é que o falso "quando(" não conte como o Quando exigido.
        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.motivo()).contains("Quando=false");
    }

    @Test
    @DisplayName("validarRepresentacaoFinal: cenário completo pós-Formatter (Então já movido para Resultado Esperado) deve ser VALID")
    void validarRepresentacaoFinalDeveAceitarCenarioCompletoPosFormatter() {
        CenarioItem item = new CenarioItem();
        item.setNome("Login com credenciais válidas");
        item.setScriptTeste("Dado que o usuário está na tela de login\nQuando ele informa credenciais válidas");
        item.setResultadoEsperado("Então o login é realizado com sucesso");

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarRepresentacaoFinal(item);

        assertThat(resultado.valido()).isTrue();
    }

    @Test
    @DisplayName("validarRepresentacaoFinal: deve rejeitar cenário com Passos vazio")
    void validarRepresentacaoFinalDeveRejeitarPassosVazio() {
        CenarioItem item = new CenarioItem();
        item.setNome("Login com credenciais válidas");
        item.setScriptTeste("");
        item.setResultadoEsperado("Então o login é realizado com sucesso");

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarRepresentacaoFinal(item);

        assertThat(resultado.valido()).isFalse();
    }

    @Test
    @DisplayName("validarRepresentacaoFinal: deve rejeitar cenário com Resultado Esperado vazio")
    void validarRepresentacaoFinalDeveRejeitarResultadoEsperadoVazio() {
        CenarioItem item = new CenarioItem();
        item.setNome("Login com credenciais válidas");
        item.setScriptTeste("Dado que o usuário está na tela de login\nQuando ele informa credenciais válidas");
        item.setResultadoEsperado("");

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarRepresentacaoFinal(item);

        assertThat(resultado.valido()).isFalse();
    }

    @Test
    @DisplayName("validarRepresentacaoFinal: deve rejeitar cenário sem nome")
    void validarRepresentacaoFinalDeveRejeitarSemNome() {
        CenarioItem item = new CenarioItem();
        item.setScriptTeste("Dado que o usuário está na tela de login\nQuando ele informa credenciais válidas");
        item.setResultadoEsperado("Então o login é realizado com sucesso");

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarRepresentacaoFinal(item);

        assertThat(resultado.valido()).isFalse();
    }

    @Test
    @DisplayName("validarRepresentacaoFinal: deve rejeitar cenário sem Dado ou sem Quando nos Passos")
    void validarRepresentacaoFinalDeveRejeitarSemDadoOuQuando() {
        CenarioItem item = new CenarioItem();
        item.setNome("Login com credenciais válidas");
        item.setScriptTeste("O usuário acessa a tela e informa os dados"); // sem Dado/Quando
        item.setResultadoEsperado("Então o login é realizado com sucesso");

        GeneratedScenariosValidator.ValidationResult resultado = validator.validarRepresentacaoFinal(item);

        assertThat(resultado.valido()).isFalse();
    }

    // ===== FASE15-BUG-003A: item pós-Reviewer que não é um cenário real =====

    @Test
    @DisplayName("pareceConteudoNaoCenario: item com Passos e Resultado Esperado vazios deve ser identificado como conteúdo não-cenário")
    void pareceConteudoNaoCenarioDeveIdentificarItemTotalmenteVazio() {
        CenarioItem item = new CenarioItem();
        item.setNome("Observações de otimização:");
        item.setScriptTeste("");
        item.setResultadoEsperado("");

        assertThat(validator.pareceConteudoNaoCenario(item)).isTrue();
    }

    @Test
    @DisplayName("pareceConteudoNaoCenario: item com conteúdo real em Passos NÃO deve ser identificado como não-cenário, mesmo com nome incomum")
    void pareceConteudoNaoCenarioNaoDeveDescartarCenarioRealComNomeIncomum() {
        CenarioItem item = new CenarioItem();
        item.setNome("Resumo do fluxo de login");
        item.setScriptTeste("Dado que o usuário está na tela de login\nQuando ele informa credenciais válidas");
        item.setResultadoEsperado("Então o login é realizado com sucesso");

        assertThat(validator.pareceConteudoNaoCenario(item)).isFalse();
    }

    // ===== FASE15-BUG-005B: validação estrutural de evidência =====

    @Test
    @DisplayName("validarEstruturaEvidencia: item sem evidenceType (formato anterior ao BUG-005B) deve ser considerado válido (retrocompatibilidade)")
    void validarEstruturaEvidenciaDeveAceitarItemSemEvidenceType() {
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário legado");

        assertThat(validator.validarEstruturaEvidencia(item).valido()).isTrue();
    }

    @Test
    @DisplayName("validarEstruturaEvidencia: evidenceType desconhecido deve ser inválido")
    void validarEstruturaEvidenciaDeveRejeitarTipoDesconhecido() {
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário");
        item.setEvidenceType("TALVEZ");

        assertThat(validator.validarEstruturaEvidencia(item).valido()).isFalse();
    }

    @Test
    @DisplayName("validarEstruturaEvidencia: EXPLORATORY com Status APPROVED deve ser inválido")
    void validarEstruturaEvidenciaDeveRejeitarExploratoryComApproved() {
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário");
        item.setEvidenceType("EXPLORATORY");
        item.setStatus("APPROVED");

        assertThat(validator.validarEstruturaEvidencia(item).valido()).isFalse();
    }

    @Test
    @DisplayName("validarEstruturaEvidencia: EXPLORATORY com Status REVIEW_REQUIRED deve ser válido")
    void validarEstruturaEvidenciaDeveAceitarExploratoryComReviewRequired() {
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário");
        item.setEvidenceType("EXPLORATORY");
        item.setStatus("REVIEW_REQUIRED");
        item.setEvidenceSources("Não se aplica");

        assertThat(validator.validarEstruturaEvidencia(item).valido()).isTrue();
    }

    @Test
    @DisplayName("validarEstruturaEvidencia: DOCUMENTED com evidenceSources vazio deve ser inválido")
    void validarEstruturaEvidenciaDeveRejeitarDocumentedComSourceVazia() {
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário");
        item.setEvidenceType("DOCUMENTED");
        item.setStatus("APPROVED");
        item.setEvidenceSources("");

        assertThat(validator.validarEstruturaEvidencia(item).valido()).isFalse();
    }

    @Test
    @DisplayName("validarEstruturaEvidencia: DIRECT_INFERENCE com evidenceSources vazio deve ser inválido")
    void validarEstruturaEvidenciaDeveRejeitarDirectInferenceComSourceVazia() {
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário");
        item.setEvidenceType("DIRECT_INFERENCE");
        item.setStatus("APPROVED");
        item.setEvidenceSources(null);

        assertThat(validator.validarEstruturaEvidencia(item).valido()).isFalse();
    }

    @Test
    @DisplayName("validarEstruturaEvidencia: DOCUMENTED com evidenceSources preenchido deve ser válido")
    void validarEstruturaEvidenciaDeveAceitarDocumentedComSourcePreenchida() {
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário");
        item.setEvidenceType("DOCUMENTED");
        item.setStatus("APPROVED");
        item.setEvidenceSources("RN-B-02");

        assertThat(validator.validarEstruturaEvidencia(item).valido()).isTrue();
    }

    // ===== FASE15-BUG-005B: checagem determinística de existência de fonte =====

    @Test
    @DisplayName("fonteExisteNoTextoBruto: source presente literalmente no texto bruto deve ser encontrada")
    void fonteExisteNoTextoBrutoDeveEncontrarSourceExistente() {
        String bruto = "RN-A-01: telefone obrigatório. RN-A-02: DDD obrigatório.";

        assertThat(validator.fonteExisteNoTextoBruto("RN-A-02", bruto)).isTrue();
    }

    @Test
    @DisplayName("fonteExisteNoTextoBruto: source inexistente no texto bruto não deve ser encontrada")
    void fonteExisteNoTextoBrutoNaoDeveEncontrarSourceInexistente() {
        String bruto = "RN-A-01: telefone obrigatório. RN-A-02: DDD obrigatório.";

        assertThat(validator.fonteExisteNoTextoBruto("RN-A-99", bruto)).isFalse();
    }

    @Test
    @DisplayName("fonteExisteNoTextoBruto: 'USER' é sempre uma referência válida quando existe regra digitada")
    void fonteExisteNoTextoBrutoDeveAceitarUserComoReferenciaEspecial() {
        String bruto = "O sistema deve permitir cadastrar um cliente.";

        assertThat(validator.fonteExisteNoTextoBruto("USER", bruto)).isTrue();
    }

    @Test
    @DisplayName("fonteExisteNoTextoBruto: 'USER' não é válido quando não há regra bruta alguma")
    void fonteExisteNoTextoBrutoDeveRejeitarUserSemRegraBruta() {
        assertThat(validator.fonteExisteNoTextoBruto("USER", "")).isFalse();
        assertThat(validator.fonteExisteNoTextoBruto("USER", null)).isFalse();
    }

    // ===== FASE15-BUG-005B: downgrade determinístico quando source não existe =====

    @Test
    @DisplayName("corrigirSourceInexistente: DOCUMENTED com source inexistente deve ser rebaixado para EXPLORATORY/REVIEW_REQUIRED")
    void corrigirSourceInexistenteDeveRebaixarQuandoSourceNaoExiste() {
        CenarioItem item = new CenarioItem();
        item.setNome("Cadastro com CEP oculto no exterior");
        item.setEvidenceType("DOCUMENTED");
        item.setStatus("APPROVED");
        item.setEvidenceSources("RN-A-99");

        validator.corrigirSourceInexistente(item, "RN-A-01: telefone. RN-A-02: DDD. RN-B-01: CEP.");

        assertThat(item.getEvidenceType()).isEqualTo("EXPLORATORY");
        assertThat(item.getStatus()).isEqualTo("REVIEW_REQUIRED");
    }

    @Test
    @DisplayName("corrigirSourceInexistente: DOCUMENTED com source existente NÃO deve ser alterado")
    void corrigirSourceInexistenteNaoDeveAlterarQuandoSourceExiste() {
        CenarioItem item = new CenarioItem();
        item.setNome("Cadastro com CEP inválido");
        item.setEvidenceType("DOCUMENTED");
        item.setStatus("APPROVED");
        item.setEvidenceSources("RN-B-02");

        validator.corrigirSourceInexistente(item, "RN-B-02: CEP deve possuir exatamente 8 dígitos.");

        assertThat(item.getEvidenceType()).isEqualTo("DOCUMENTED");
        assertThat(item.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("corrigirSourceInexistente: múltiplas sources, uma inexistente, deve rebaixar")
    void corrigirSourceInexistenteDeveRebaixarQuandoUmaDasVariasSourcesNaoExiste() {
        CenarioItem item = new CenarioItem();
        item.setNome("Cadastro combinando duas regras");
        item.setEvidenceType("DOCUMENTED");
        item.setStatus("APPROVED");
        item.setEvidenceSources("RN-A-01, RN-A-99");

        validator.corrigirSourceInexistente(item, "RN-A-01: telefone obrigatório.");

        assertThat(item.getEvidenceType()).isEqualTo("EXPLORATORY");
        assertThat(item.getStatus()).isEqualTo("REVIEW_REQUIRED");
    }

    @Test
    @DisplayName("corrigirSourceInexistente: EXPLORATORY não é afetado (checagem só se aplica a DOCUMENTED/DIRECT_INFERENCE)")
    void corrigirSourceInexistenteNaoDeveAlterarExploratory() {
        CenarioItem item = new CenarioItem();
        item.setNome("Cenário exploratório");
        item.setEvidenceType("EXPLORATORY");
        item.setStatus("REVIEW_REQUIRED");
        item.setEvidenceSources("Não se aplica");

        validator.corrigirSourceInexistente(item, "qualquer texto");

        assertThat(item.getEvidenceType()).isEqualTo("EXPLORATORY");
        assertThat(item.getStatus()).isEqualTo("REVIEW_REQUIRED");
    }
}
