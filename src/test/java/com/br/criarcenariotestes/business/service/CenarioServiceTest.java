package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.dto.CenarioResponse;
import com.br.criarcenariotestes.business.fallback.CenarioFallbackFactory;
import com.br.criarcenariotestes.business.workflow.QaWorkflowService;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import com.br.criarcenariotestes.business.document.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CenarioService - Testes Unitários (Refatorado BMAD)")
class CenarioServiceTest {

    @Mock
    private QaWorkflowService qaWorkflowService;

    @Mock
    private CenarioRepository cenarioRepository;

    @Mock
    private CenarioFallbackFactory fallbackFactory;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    @InjectMocks
    private CenarioService service;

    private CenarioRequest request;
    private CenarioResponse expectedResponse;

    @BeforeEach
    void setUp() {
        request = new CenarioRequest(
                "Login OAuth",
                "Sistema de login",
                "gerador_cenarios_testes"
        );

        expectedResponse = new CenarioResponse(
                "123",
                "Login OAuth",
                "Sistema de login",
                "Critérios de aceitação",
                List.of(new CenarioItem())
        );
    }

    @Test
    @DisplayName("Deve gerar cenário completo via BMAD workflow")
    void deveGerarCenarioCompletoViaBMAD() {
        // Arrange
        when(qaWorkflowService.executarWorkflow(any(CenarioRequest.class)))
                .thenReturn(expectedResponse);

        // Act
        CenarioResponse response = service.gerarCenarioCompleto(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("123");
        assertThat(response.titulo()).isEqualTo("Login OAuth");

        verify(qaWorkflowService, times(1)).executarWorkflow(request);
        verify(fallbackFactory, never()).criar(any());
    }

    @Test
    @DisplayName("Deve aplicar fallback quando workflow falhar")
    void deveAplicarFallbackQuandoWorkflowFalhar() {
        // Arrange
        when(qaWorkflowService.executarWorkflow(any()))
                .thenThrow(new RuntimeException("Erro no workflow"));

        Cenario cenarioFallback = new Cenario();
        cenarioFallback.setId("fallback-123");
        cenarioFallback.setTitulo("Login OAuth");
        cenarioFallback.setCenarios(List.of(new CenarioItem()));

        when(fallbackFactory.criar(any())).thenReturn(cenarioFallback);
        when(cenarioRepository.save(any(Cenario.class))).thenReturn(cenarioFallback);

        // Act
        CenarioResponse response = service.gerarCenarioCompleto(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("fallback-123");

        verify(qaWorkflowService, times(1)).executarWorkflow(any());
        verify(fallbackFactory, times(1)).criar(request);
        verify(cenarioRepository, times(1)).save(any(Cenario.class));
    }

    @Test
    @DisplayName("Deve listar todos os cenários")
    void deveListarTodosCenarios() {
        // Arrange
        Cenario cenario1 = new Cenario();
        cenario1.setId("1");
        Cenario cenario2 = new Cenario();
        cenario2.setId("2");

        when(cenarioRepository.findAll()).thenReturn(List.of(cenario1, cenario2));

        // Act
        List<Cenario> cenarios = service.listarCenarios();

        // Assert
        assertThat(cenarios).hasSize(2);
        verify(cenarioRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar cenário por ID")
    void deveBuscarCenarioPorId() {
        // Arrange
        Cenario cenario = new Cenario();
        cenario.setId("123");
        cenario.setTitulo("Login OAuth");

        when(cenarioRepository.findById("123")).thenReturn(Optional.of(cenario));

        // Act
        Cenario resultado = service.buscarCenario("123");

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo("123");
        assertThat(resultado.getTitulo()).isEqualTo("Login OAuth");

        verify(cenarioRepository, times(1)).findById("123");
    }

    @Test
    @DisplayName("Deve excluir cenário por ID")
    void deveExcluirCenarioPorId() {
        // Arrange
        doNothing().when(cenarioRepository).deleteById("123");

        // Act
        service.excluirCenario("123");

        // Assert
        verify(cenarioRepository, times(1)).deleteById("123");
    }
}
