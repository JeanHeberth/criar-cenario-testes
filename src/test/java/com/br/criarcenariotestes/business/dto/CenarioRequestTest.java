package com.br.criarcenariotestes.business.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CenarioRequest - contrato público (JSON)")
class CenarioRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Deve aceitar os nomes novos, neutros de rastreador")
    void deveAceitarNomesNovos() throws Exception {
        String json = """
                {
                  "titulo": "Login",
                  "regraDeNegocio": "regra",
                  "agent": "gerador_cenarios_testes",
                  "taskRef": "https://empresa.atlassian.net/browse/SCRUM-28",
                  "pastaDestino": "Java",
                  "projectKey": "QA"
                }
                """;

        CenarioRequest request = mapper.readValue(json, CenarioRequest.class);

        assertEquals("https://empresa.atlassian.net/browse/SCRUM-28", request.taskRef());
        assertEquals("Java", request.pastaDestino());
        assertEquals("QA", request.projectKey());
    }

    @Test
    @DisplayName("Deve continuar aceitando os nomes antigos - front, Jenkins e testes não quebram na renomeação")
    void deveAceitarNomesAntigosPorAlias() throws Exception {
        String json = """
                {
                  "titulo": "Login",
                  "regraDeNegocio": "regra",
                  "agent": "gerador_cenarios_testes",
                  "jiraIssueKey": "SCRUM-28",
                  "pastaRaiz": "Java"
                }
                """;

        CenarioRequest request = mapper.readValue(json, CenarioRequest.class);

        assertEquals("SCRUM-28", request.taskRef(), "jiraIssueKey deve alimentar taskRef");
        assertEquals("Java", request.pastaDestino(), "pastaRaiz deve alimentar pastaDestino");
    }

    @Test
    @DisplayName("Campos de destino são todos opcionais")
    void camposDeDestinoSaoOpcionais() throws Exception {
        String json = """
                {"titulo": "Login", "regraDeNegocio": "regra", "agent": "gerador_cenarios_testes"}
                """;

        CenarioRequest request = mapper.readValue(json, CenarioRequest.class);

        assertNull(request.taskRef());
        assertNull(request.pastaDestino());
        assertNull(request.projectKey());
    }
}
