package com.br.criarcenariotestes.business.fallback;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CenarioFallbackFactoryTest {

    private final CenarioFallbackFactory factory = new CenarioFallbackFactory();

    @Test
    void deveGerarMaisDeDoisCenariosNoFallback() {
        CenarioRequest request = new CenarioRequest(
                "Login",
                "1. Permitir login com credenciais válidas\n2. Bloquear senha inválida\n3. Exigir MFA quando habilitado",
                null
        );

        Cenario cenario = factory.criar(request);

        assertEquals(8, cenario.getCenarios().size());
        assertTrue(cenario.getCenarios().stream().anyMatch(item -> item.getNome().contains("Fluxo principal - Login")));
        assertTrue(cenario.getCenarios().stream().anyMatch(item -> item.getNome().contains("Cobertura de critério 1 - Login")));
        assertTrue(cenario.getCenarios().stream().anyMatch(item -> item.getNome().contains("Cobertura de critério 3 - Login")));
    }
}
