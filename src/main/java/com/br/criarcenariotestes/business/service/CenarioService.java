package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.ai.AiProviderResolver;
import com.br.criarcenariotestes.business.document.PdfTextExtractor;
import com.br.criarcenariotestes.business.dto.CenarioRequest;
import com.br.criarcenariotestes.business.dto.CenarioResponse;
import com.br.criarcenariotestes.business.fallback.CenarioFallbackFactory;
import com.br.criarcenariotestes.business.parser.CenarioTextoParser;
import com.br.criarcenariotestes.business.prompt.PromptFactory;
import com.br.criarcenariotestes.infrastructure.entity.Cenario;
import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import com.br.criarcenariotestes.infrastructure.repository.CenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CenarioService {

    private static final int MAX_TENTATIVAS = 2;
    private static final long TEMPO_ESPERA_MS = 2000L;
    private final PdfTextExtractor pdfTextExtractor;


    private final CenarioRepository cenarioRepository;
    private final AiProviderResolver aiProviderResolver;
    private final CenarioTextoParser cenarioTextoParser;
    private final CenarioFallbackFactory fallbackFactory;

    public CenarioResponse gerarCenarioCompleto(CenarioRequest request) {
        String systemPrompt = PromptFactory.getSystemPrompt();
        String userPrompt = PromptFactory.getUserPrompt(request.titulo(), request.regraDeNegocio());

        try {
            AiProvider provider = aiProviderResolver.getActiveProvider();
            String resposta = tentarComRetry(provider, systemPrompt, userPrompt);
            return salvarResposta(request, resposta);
        } catch (Exception e) {
            System.err.println("⚠️ Provider principal falhou: " + e.getMessage());
        }

        try {
            AiProvider fallbackProvider = aiProviderResolver.getFallbackProvider();
            String resposta = tentarComRetry(fallbackProvider, systemPrompt, userPrompt);
            return salvarResposta(request, resposta);
        } catch (Exception e) {
            System.err.println("⚠️ Provider fallback falhou: " + e.getMessage());
        }

        return salvarFallback(request);
    }

    private String tentarComRetry(AiProvider provider, String systemPrompt, String userPrompt) {
        Exception ultimaExcecao = null;

        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                return provider.gerarResposta(systemPrompt, userPrompt);
            } catch (Exception e) {
                ultimaExcecao = e;

                if (tentativa < MAX_TENTATIVAS) {
                    aguardar();
                }
            }
        }

        throw new RuntimeException("Todas as tentativas falharam para " + provider.getName(), ultimaExcecao);
    }

    private CenarioResponse salvarResposta(CenarioRequest request, String respostaIa) {
        List<CenarioItem> itens = cenarioTextoParser.parsear(respostaIa);

        if (itens.isEmpty()) {
            return salvarFallback(request);
        }

        Cenario cenario = new Cenario();
        cenario.setTitulo(request.titulo());
        cenario.setRegraDeNegocio(request.regraDeNegocio());
        cenario.setCriteriosAceitacao(cenarioTextoParser.extrairCriterios(respostaIa));
        cenario.setCenarios(itens);

        Cenario salvo = cenarioRepository.save(cenario);
        return toResponse(salvo);
    }

    private CenarioResponse salvarFallback(CenarioRequest request) {
        Cenario cenario = fallbackFactory.criar(request);
        Cenario salvo = cenarioRepository.save(cenario);
        return toResponse(salvo);
    }

    private CenarioResponse toResponse(Cenario cenario) {
        return new CenarioResponse(
                cenario.getId(),
                cenario.getTitulo(),
                cenario.getRegraDeNegocio(),
                cenario.getCriteriosAceitacao(),
                cenario.getCenarios()
        );
    }

    private void aguardar() {
        try {
            Thread.sleep(TEMPO_ESPERA_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrompido", e);
        }
    }

    public List<Cenario> listarCenarios() {
        return cenarioRepository.findAll();
    }

    public Cenario buscarCenario(String id) {
        return cenarioRepository.findById(id).orElse(null);
    }

    public void excluirCenario(String id) {
        cenarioRepository.deleteById(id);
    }
    public CenarioResponse gerarCenarioComPdf(String titulo, String regra, MultipartFile arquivo) {

        String textoPdf;
        try {
            textoPdf = pdfTextExtractor.extrairTexto(arquivo.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar PDF", e);
        }

        String systemPrompt = PromptFactory.getSystemPrompt();
        String userPrompt = PromptFactory.getUserPromptComContexto(titulo, regra, textoPdf);

        try {
            AiProvider provider = aiProviderResolver.getActiveProvider();
            String resposta = tentarComRetry(provider, systemPrompt, userPrompt);
            return salvarResposta(new CenarioRequest(titulo, regra), resposta);
        } catch (Exception e) {
            System.err.println("⚠️ Falha com provider principal: " + e.getMessage());
        }

        try {
            AiProvider fallbackProvider = aiProviderResolver.getFallbackProvider();
            String resposta = tentarComRetry(fallbackProvider, systemPrompt, userPrompt);
            return salvarResposta(new CenarioRequest(titulo, regra), resposta);
        } catch (Exception e) {
            System.err.println("⚠️ Falha com fallback: " + e.getMessage());
        }

        return salvarFallback(new CenarioRequest(titulo, regra));
    }
}