package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.dto.AgentInfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgentLoaderService {

    private static final Logger log = LoggerFactory.getLogger(AgentLoaderService.class);
    private static final String AGENTS_DIR = "agents";

    @Value("${agents.directory:}")
    private String configuredAgentsDir;

    public List<AgentInfoResponse> listAgents() {
        List<AgentInfoResponse> agents = new ArrayList<>();
        Path agentsPath = resolveAgentsDirectory();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(agentsPath, "*.agent.md")) {
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();
                String id = fileName.replace(".agent.md", "");
                agents.add(new AgentInfoResponse(id, fileName));
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao listar agentes em: " + agentsPath.toAbsolutePath(), e);
        }

        return agents;
    }

    public String loadAgentInstructions(String agentId) {
        Path agentsPath = resolveAgentsDirectory();
        Path path = agentsPath.resolve(agentId + ".agent.md");

        if (!Files.exists(path)) {
            throw new RuntimeException("Agente nao encontrado: " + agentId + " em " + agentsPath.toAbsolutePath());
        }

        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler agente: " + agentId, e);
        }
    }

    private Path resolveAgentsDirectory() {
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

        throw new RuntimeException("Diretorio de agentes nao encontrado. user.dir=" + userDir);
    }
}
