package com.br.criarcenariotestes.business.fallback;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CenarioFallbackFactory {

    public Cenario criar(CenarioRequest request) {
        Cenario cenario = new Cenario();
        cenario.setTitulo(request.titulo());
        cenario.setRegraDeNegocio(request.regraDeNegocio());
        cenario.setCriteriosAceitacao("Cenários gerados localmente devido à indisponibilidade dos provedores de IA.");
        cenario.setCenarios(criarItensFallback());

        return cenario;
    }

    private List<CenarioItem> criarItensFallback() {
        return List.of(
                CenarioItem.builder()
                        .nome("Validação do fluxo principal")
                        .objetivo("Garantir que a funcionalidade execute o fluxo principal com sucesso")
                        .precondicao("Usuário com dados válidos e ambiente disponível")
                        .scriptTeste("""
                                Dado que o usuário possui dados válidos
                                Quando executa a funcionalidade
                                Então o sistema deve processar a operação corretamente
                                """.trim())
                        .resultadoEsperado("O sistema deve concluir a operação sem erros.")
                        .componente("Funcionalidade principal")
                        .rotulos("regressao, fluxo-principal")
                        .proposito("TESTE MANUAL")
                        .pasta("Funcionalidade > Fluxo Principal")
                        .proprietario("JIRAUSER23105")
                        .cobertura("Fluxo principal")
                        .status("APPROVED")
                        .build(),

                CenarioItem.builder()
                        .nome("Validação de regra de negócio")
                        .objetivo("Garantir que a regra de negócio seja aplicada corretamente")
                        .precondicao("Regra de negócio disponível para validação")
                        .scriptTeste("""
                                Dado que existe uma regra de negócio definida
                                Quando a funcionalidade é executada
                                Então o sistema deve respeitar a regra configurada
                                """.trim())
                        .resultadoEsperado("A operação deve ser permitida ou bloqueada conforme a regra.")
                        .componente("Regra de negócio")
                        .rotulos("regressao, regra-negocio")
                        .proposito("TESTE MANUAL")
                        .pasta("Funcionalidade > Regra de Negócio")
                        .proprietario("JIRAUSER23105")
                        .cobertura("Regra de negócio")
                        .status("APPROVED")
                        .build()
        );
    }
}