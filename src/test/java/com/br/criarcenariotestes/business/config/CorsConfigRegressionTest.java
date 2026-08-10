package com.br.criarcenariotestes.business.config;

import com.br.criarcenariotestes.business.service.CenarioService;
import com.br.criarcenariotestes.controller.CenarioController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prova de regressão: como CorsConfig é global (mapping "/**"), a mesma
 * política precisa valer também para controllers fora do Auto QA — não
 * basta provar que funciona só em AutoQaExecutionController.
 */
@WebMvcTest(controllers = CenarioController.class)
@Import({CorsConfig.class, AppCorsProperties.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:4200")
@DisplayName("CorsConfig - Regressão em controller não-Auto QA (Fase 13.1B)")
class CorsConfigRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CenarioService cenarioService;

    @Test
    @DisplayName("CenarioController (fora do Auto QA) também autoriza a origem configurada globalmente")
    void cenarioControllerTambemAutorizaOrigemConfigurada() throws Exception {
        mockMvc.perform(get("/cenario/workflows").header(HttpHeaders.ORIGIN, "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    @DisplayName("CenarioController (fora do Auto QA) também rejeita origem não autorizada")
    void cenarioControllerTambemRejeitaOrigemNaoAutorizada() throws Exception {
        mockMvc.perform(get("/cenario/workflows").header(HttpHeaders.ORIGIN, "http://evil.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
