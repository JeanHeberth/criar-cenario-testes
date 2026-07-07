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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CenarioService {

    private static final Logger log = LoggerFactory.getLogger(CenarioService.class);

    private static final int MAX_TENTATIVAS = 2;
    private static final long TEMPO_ESPERA_MS = 2000L;
    private final PdfTextExtractor pdfTextExtractor;

    private final CenarioRepository cenarioRepository;
    private final AiProviderResolver aiProviderResolver;
    private final CenarioTextoParser cenarioTextoParser;
    private final CenarioFallbackFactory fallbackFactory;
    private final AgentLoaderService agentLoaderService;

    public CenarioResponse gerarCenarioCompleto(CenarioRequest request) {
        String systemPrompt = buildSystemPrompt(request.agent());
        String userPrompt = PromptFactory.getUserPrompt(request.titulo(), request.regraDeNegocio());

        log.info("Iniciando geracao de cenarios. titulo='{}', agent='{}', systemPromptLength={}, userPromptLength={}",
                request.titulo(),
                request.agent(),
                tamanho(systemPrompt),
                tamanho(userPrompt));

        try {
            AiProvider provider = aiProviderResolver.getActiveProvider();
            log.info("Chamando provider principal: {}", provider.getName());
            String resposta = tentarComRetry(provider, systemPrompt, userPrompt);
            return salvarResposta(request, resposta, provider.getName());
        } catch (Exception e) {
            log.warn("Provider principal falhou. titulo='{}', agent='{}', erro='{}'",
                    request.titulo(), request.agent(), e.getMessage(), e);
        }

        try {
            AiProvider fallbackProvider = aiProviderResolver.getFallbackProvider();
            log.info("Chamando provider fallback: {}", fallbackProvider.getName());
            String resposta = tentarComRetry(fallbackProvider, systemPrompt, userPrompt);
            return salvarResposta(request, resposta, fallbackProvider.getName());
        } catch (Exception e) {
            log.warn("Provider fallback falhou. titulo='{}', agent='{}', erro='{}'",
                    request.titulo(), request.agent(), e.getMessage(), e);
        }

        return salvarFallback(request);
    }

    private String tentarComRetry(AiProvider provider, String systemPrompt, String userPrompt) {
        Exception ultimaExcecao = null;

        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                log.info("Tentativa {} de {} no provider {}",
                        tentativa,
                        MAX_TENTATIVAS,
                        provider.getName());
                return provider.gerarResposta(systemPrompt, userPrompt);
            } catch (Exception e) {
                ultimaExcecao = e;
                log.warn("Tentativa {} falhou no provider {}: {}",
                        tentativa,
                        provider.getName(),
                        e.getMessage());

                if (tentativa < MAX_TENTATIVAS) {
                    aguardar();
                }
            }
        }

        throw new RuntimeException("Todas as tentativas falharam para " + provider.getName(), ultimaExcecao);
    }

    private CenarioResponse salvarResposta(CenarioRequest request, String respostaIa, String providerName) {
        List<CenarioItem> itens = cenarioTextoParser.parsear(respostaIa);

        log.info("Resposta recebida do provider {}. titulo='{}', responseLength={}, blocosSeparados={}, cenariosParseados={}, preview='{}'",
                providerName,
                request.titulo(),
                tamanho(respostaIa),
                contarBlocos(respostaIa),
                itens.size(),
                gerarPreview(respostaIa));

        if (!itens.isEmpty()) {
            log.info("Cenarios parseados: {}", extrairNomes(itens));
        }

        if (itens.isEmpty()) {
            log.warn("Nenhum cenario foi parseado da resposta do provider {}. Aplicando fallback. titulo='{}'",
                    providerName,
                    request.titulo());
            return salvarFallback(request);
        }

        if (itens.size() <= 2) {
            log.warn("Quantidade baixa de cenarios retornada pelo provider {}. titulo='{}', quantidade={}, nomes={}",
                    providerName,
                    request.titulo(),
                    itens.size(),
                    extrairNomes(itens));
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
        log.warn("Gerando cenarios via fallback local. titulo='{}', agent='{}'",
                request.titulo(),
                request.agent());
        Cenario cenario = fallbackFactory.criar(request);
        Cenario salvo = cenarioRepository.save(cenario);
        log.info("Fallback gerou {} cenarios. titulo='{}'",
                cenario.getCenarios() == null ? 0 : cenario.getCenarios().size(),
                request.titulo());
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

    public CenarioResponse gerarCenarioComPdf(
            String titulo,
            String regra,
            String agent,
            List<MultipartFile> arquivos
    ) {

        StringBuilder contextoCompleto = new StringBuilder();

        for (MultipartFile arquivo : arquivos) {
            try {
                String texto = pdfTextExtractor.extrairTexto(arquivo.getInputStream());

                contextoCompleto.append("\n\n### DOCUMENTO: ")
                        .append(arquivo.getOriginalFilename())
                        .append("\n")
                        .append(texto);

            } catch (Exception e) {
                throw new RuntimeException("Erro ao processar PDF: " + arquivo.getOriginalFilename(), e);
            }
        }

        String systemPrompt = buildSystemPrompt(agent);
        String userPrompt = PromptFactory.getUserPromptComContexto(
                titulo,
                regra,
                contextoCompleto.toString()
        );

        try {
            AiProvider provider = aiProviderResolver.getActiveProvider();
            log.info("Chamando provider principal com PDF: {}", provider.getName());
            String resposta = tentarComRetry(provider, systemPrompt, userPrompt);
            return salvarResposta(new CenarioRequest(titulo, regra, agent), resposta, provider.getName());
        } catch (Exception e) {
            log.warn("Provider principal falhou na geracao com PDF. titulo='{}', agent='{}', erro='{}'",
                    titulo, agent, e.getMessage(), e);
        }

        try {
            AiProvider fallbackProvider = aiProviderResolver.getFallbackProvider();
            log.info("Chamando provider fallback com PDF: {}", fallbackProvider.getName());
            String resposta = tentarComRetry(fallbackProvider, systemPrompt, userPrompt);
            return salvarResposta(new CenarioRequest(titulo, regra, agent), resposta, fallbackProvider.getName());
        } catch (Exception e) {
            log.warn("Provider fallback falhou na geracao com PDF. titulo='{}', agent='{}', erro='{}'",
                    titulo, agent, e.getMessage(), e);
        }

        return salvarFallback(new CenarioRequest(titulo, regra, agent));
    }

    private String buildSystemPrompt(String agent) {
        String basePrompt = PromptFactory.getSystemPrompt();

        if (agent == null || agent.isBlank()) {
            return basePrompt;
        }

        String instrucoesAgente = agentLoaderService.loadAgentInstructions(agent);
        log.info("System prompt montado com instrucoes do agente '{}'. baseLength={}, agentInstructionsLength={}",
                agent,
                tamanho(basePrompt),
                tamanho(instrucoesAgente));
        return instrucoesAgente + "\n\n---\n\n" + basePrompt;
    }

    private int tamanho(String valor) {
        return valor == null ? 0 : valor.length();
    }

    private int contarBlocos(String respostaIa) {
        if (respostaIa == null || respostaIa.isBlank()) {
            return 0;
        }

        return respostaIa.split("(?m)^\\s*---+\\s*$").length;
    }

    private String gerarPreview(String respostaIa) {
        if (respostaIa == null || respostaIa.isBlank()) {
            return "";
        }

        String normalizado = respostaIa.replaceAll("\\s+", " ").trim();
        return normalizado.length() <= 300 ? normalizado : normalizado.substring(0, 300) + "...";
    }

    private List<String> extrairNomes(List<CenarioItem> itens) {
        List<String> nomes = new ArrayList<>();

        for (CenarioItem item : itens) {
            if (item != null && item.getNome() != null && !item.getNome().isBlank()) {
                nomes.add(item.getNome());
            }
        }

        return nomes;
    }
}