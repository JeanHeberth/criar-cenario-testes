package com.br.criarcenariotestes.business.fallback;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class CenarioFallbackFactory {

    private static final int MAX_CENARIOS_POR_CRITERIO = 10;

    public Cenario criar(CenarioRequest request) {
        Cenario cenario = new Cenario();
        cenario.setTitulo(request.titulo());
        cenario.setRegraDeNegocio(request.regraDeNegocio());
        cenario.setCriteriosAceitacao("Cenários gerados localmente devido à indisponibilidade dos provedores de IA.");
        cenario.setCenarios(criarItensFallback(request));

        return cenario;
    }

    private List<CenarioItem> criarItensFallback(CenarioRequest request) {
        String titulo = valorOuPadrao(request.titulo(), "Funcionalidade");
        String regra = valorOuPadrao(request.regraDeNegocio(), "Regra de negócio");

        List<CenarioItem> cenarios = new ArrayList<>();

        cenarios.add(construirItem(
                "Fluxo principal - " + titulo,
                "Garantir a execução correta do fluxo principal da funcionalidade.",
                "Usuário autenticado, dados válidos e ambiente disponível.",
                """
                        Dado que o usuário está apto a executar a funcionalidade
                        E os dados obrigatórios estão preenchidos
                        Quando executa o fluxo principal
                        Então o sistema processa a operação com sucesso
                        """.trim(),
                "Operação concluída com sucesso, sem erros e com persistência consistente dos dados.",
                "Funcionalidade principal",
                "regressao, fluxo-principal, positivo",
                "Cobrir o caminho de maior valor da funcionalidade.",
                "Funcionalidade > Fluxo Principal",
                "Fluxo principal"
        ));

        cenarios.add(construirItem(
                "Validação de regra de negócio - " + titulo,
                "Garantir o cumprimento das regras descritas para a feature.",
                "Regra de negócio configurada e dados de entrada compatíveis.",
                """
                        Dado que existe regra de negócio definida para a funcionalidade
                        Quando a operação é executada conforme a regra
                        Então o sistema aplica corretamente o comportamento esperado
                        """.trim(),
                "Resultado final aderente ao comportamento definido na regra de negócio.",
                "Regra de negócio",
                "regressao, regra-negocio, positivo",
                "Evitar desvio funcional em produção.",
                "Funcionalidade > Regra de Negocio",
                "Regra de negócio"
        ));

        cenarios.add(construirItem(
                "Dados inválidos e validações - " + titulo,
                "Garantir tratamento de entradas inválidas e mensagens claras ao usuário.",
                "Tela/API disponível e validações habilitadas.",
                """
                        Dado que o usuário informa dados inválidos ou inconsistentes
                        Quando tenta concluir a operação
                        Então o sistema bloqueia o processamento e informa o motivo da rejeição
                        """.trim(),
                "Nenhum dado inválido é persistido e a mensagem de validação é apresentada.",
                "Validações",
                "regressao, negativo, validacao",
                "Reduzir incidentes causados por dados incorretos.",
                "Funcionalidade > Validacoes",
                "Validações"
        ));

        cenarios.add(construirItem(
                "Borda de dados e campos obrigatórios - " + titulo,
                "Validar comportamento em limites mínimos, máximos e ausência de campos obrigatórios.",
                "Campos da funcionalidade expostos para preenchimento.",
                """
                        Dado que o usuário preenche dados nos limites permitidos
                        E omite ao menos um campo obrigatório
                        Quando tenta avançar no fluxo
                        Então o sistema impede a continuidade e sinaliza os campos pendentes
                        """.trim(),
                "Sistema mantém integridade dos dados e valida corretamente os limites definidos.",
                "Validações",
                "regressao, borda, obrigatoriedade",
                "Cobrir falhas comuns em fronteiras de entrada.",
                "Funcionalidade > Borda",
                "Borda e obrigatoriedade"
        ));

        cenarios.add(construirItem(
                "Permissão e perfil de acesso - " + titulo,
                "Garantir que apenas perfis autorizados executem ações sensíveis.",
                "Existem perfis com e sem permissão para a ação.",
                """
                        Dado que um usuário sem permissão tenta executar a funcionalidade
                        Quando aciona a operação restrita
                        Então o sistema nega acesso e registra tentativa não autorizada
                        """.trim(),
                "Ação bloqueada para perfil não autorizado e permitida para perfil elegível.",
                "Segurança",
                "regressao, seguranca, permissao",
                "Evitar falhas de autorização e exposição indevida.",
                "Funcionalidade > Permissoes",
                "Controle de acesso"
        ));

        List<String> criterios = extrairTopicosRegra(regra);
        int limite = Math.min(criterios.size(), MAX_CENARIOS_POR_CRITERIO);

        for (int i = 0; i < limite; i++) {
            String criterio = criterios.get(i);
            cenarios.add(construirItem(
                    "Cobertura de critério " + (i + 1) + " - " + titulo,
                    "Validar especificamente o critério: " + criterio,
                    "Critério de aceite identificado e dados base preparados.",
                    """
                            Dado que o critério de aceite está configurado para validação
                            Quando o usuário executa a funcionalidade considerando este critério
                            Então o comportamento observado deve respeitar exatamente a regra definida
                            """.trim(),
                    "O critério é atendido sem violar validações, permissões ou consistência de dados.",
                    "Criterios de aceite",
                    "regressao, criterio, cobertura",
                    "Aumentar cobertura funcional do fallback sem alterar a estrutura da planilha.",
                    "Funcionalidade > Criterios de Aceite",
                    criterio
            ));
        }

        return cenarios;
    }

    private List<String> extrairTopicosRegra(String regra) {
        if (regra == null || regra.isBlank()) {
            return List.of();
        }

        List<String> topicos = new ArrayList<>();
        String[] blocos = regra.split("\\r?\\n|;|\\.");

        for (String bloco : blocos) {
            String texto = limparTopico(bloco);
            if (!texto.isBlank()) {
                topicos.add(texto);
            }
        }

        if (topicos.isEmpty()) {
            String unico = limparTopico(regra);
            if (!unico.isBlank()) {
                topicos.add(unico);
            }
        }

        return topicos;
    }

    private String limparTopico(String texto) {
        if (texto == null) {
            return "";
        }

        return Pattern.compile("^[\\-\\*\\d\\)\\(\\s]+")
                .matcher(texto.strip())
                .replaceFirst("")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private CenarioItem construirItem(
            String nome,
            String objetivo,
            String precondicao,
            String script,
            String resultado,
            String componente,
            String rotulos,
            String proposito,
            String pasta,
            String cobertura
    ) {
        return CenarioItem.builder()
                .nome(nome)
                .objetivo(objetivo)
                .precondicao(precondicao)
                .scriptTeste(script)
                .resultadoEsperado(resultado)
                .variaveis("Nao se aplica")
                .componente(componente)
                .rotulos(normalizarRotulos(rotulos))
                .proposito(proposito)
                .pasta(pasta)
                .proprietario("JIRAUSER23105")
                .cobertura(cobertura)
                .status("APPROVED")
                .build();
    }

    private String normalizarRotulos(String rotulos) {
        return valorOuPadrao(rotulos, "regressao").toLowerCase(Locale.ROOT);
    }

    private String valorOuPadrao(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}