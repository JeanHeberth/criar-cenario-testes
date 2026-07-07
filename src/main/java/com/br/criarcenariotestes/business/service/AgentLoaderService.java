package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.dto.AgentInfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AgentLoaderService {

    private static final Logger log = LoggerFactory.getLogger(AgentLoaderService.class);
    private static final String AGENTS_DIR = "agents";
    private static final String CLASSPATH_AGENTS_PATTERN = "classpath*:agents/*.agent.md";

    @Value("${agents.directory:}")
    private String configuredAgentsDir;

    public List<AgentInfoResponse> listAgents() {
        List<AgentInfoResponse> agents = new ArrayList<>();
        Path agentsPath = resolveAgentsDirectorySafely();

        if (agentsPath != null) {
            log.info("Listando agentes no diretório: {}", agentsPath.toAbsolutePath());
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(agentsPath, "*.agent.md")) {
                for (Path entry : stream) {
                    String fileName = entry.getFileName().toString();
                    String id = fileName.replace(".agent.md", "");
                    agents.add(new AgentInfoResponse(id, fileName));
                }
            } catch (IOException e) {
                throw new RuntimeException("Erro ao listar agentes em: " + agentsPath.toAbsolutePath(), e);
            }
        }

        if (agents.isEmpty()) {
            agents.addAll(listClasspathAgents());
        }

        agents.sort(Comparator.comparing(AgentInfoResponse::id));

        return agents;
    }

    public String loadAgentInstructions(String agentId) {
        Path agentsPath = resolveAgentsDirectorySafely();
        if (agentsPath != null) {
            Path path = agentsPath.resolve(agentId + ".agent.md");
            log.info("Carregando instruções do agente '{}'. path='{}'", agentId, path.toAbsolutePath());
            if (Files.exists(path)) {
                try {
                    String conteudo = Files.readString(path);
                    log.info("Agente '{}' carregado com sucesso. path='{}', length={}",
                            agentId,
                            path.toAbsolutePath(),
                            conteudo.length());
                    return conteudo;
                } catch (IOException e) {
                    throw new RuntimeException("Erro ao ler agente: " + agentId, e);
                }
            }
        }

        return loadAgentFromClasspath(agentId);
    }

    private List<AgentInfoResponse> listClasspathAgents() {
        List<AgentInfoResponse> classpathAgents = new ArrayList<>();
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(CLASSPATH_AGENTS_PATTERN);
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (fileName != null && fileName.endsWith(".agent.md")) {
                    String id = fileName.replace(".agent.md", "");
                    classpathAgents.add(new AgentInfoResponse(id, fileName));
                }
            }

            if (!classpathAgents.isEmpty()) {
                log.info("Agentes carregados via classpath: {}", classpathAgents.size());
            }

            return classpathAgents;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao listar agentes no classpath", e);
        }
    }

    private String loadAgentFromClasspath(String agentId) {
        Resource resource = new PathMatchingResourcePatternResolver()
                .getResource("classpath:agents/" + agentId + ".agent.md");

        if (!resource.exists()) {
            throw new RuntimeException("Agente nao encontrado: " + agentId);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            String conteudo = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            log.info("Agente '{}' carregado via classpath. resource='{}', length={}",
                    agentId,
                    resource.getDescription(),
                    conteudo.length());
            return conteudo;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler agente no classpath: " + agentId, e);
        }
    }

    private Path resolveAgentsDirectorySafely() {
        // 1. Tenta o path configurado via propriedade agents.directory
        if (configuredAgentsDir != null && !configuredAgentsDir.isBlank()) {
            Path configured = Paths.get(configuredAgentsDir).toAbsolutePath().normalize();
            if (Files.isDirectory(configured)) {
                log.info("Diretorio de agentes (configurado): {}", configured);
                return configured;
            }
            log.warn("Diretorio configurado nao encontrado: {}", configured);
        }

        // 2. Fallback: busca relativa ao user.dir
        Path userDir = Paths.get("").toAbsolutePath().normalize();

        List<Path> candidates = new ArrayList<>();
        candidates.add(userDir.resolve(AGENTS_DIR));
        candidates.add(userDir.resolve("api").resolve("criar-cenario-testes").resolve(AGENTS_DIR));
        candidates.add(userDir.resolve("criar-cenario-testes").resolve(AGENTS_DIR));

        Path cursor = userDir;
        while (cursor != null) {
            candidates.add(cursor.resolve(AGENTS_DIR));
            candidates.add(cursor.resolve("api").resolve("criar-cenario-testes").resolve(AGENTS_DIR));
            cursor = cursor.getParent();
        }

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                log.info("Diretorio de agentes resolvido: {}", candidate.toAbsolutePath());
                return candidate;
            }
        }

        log.warn("Diretorio externo de agentes nao encontrado. user.dir='{}', configuredAgentsDir='{}', candidates='{}'",
                userDir,
                configuredAgentsDir,
                candidates);
        return null;
    }
}
