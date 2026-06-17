package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.dto.AgentInfoResponse;
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

    private static final String AGENTS_DIR = "agents";

    public List<AgentInfoResponse> listAgents() {
        List<AgentInfoResponse> agents = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(AGENTS_DIR), "*.agent.md")) {
            for (Path entry : stream) {
                String fileName = entry.getFileName().toString();
                String id = fileName.replace(".agent.md", "");
                agents.add(new AgentInfoResponse(id, fileName));
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao listar agentes", e);
        }

        return agents;
    }

    public String loadAgentInstructions(String agentId) {
        Path path = Paths.get(AGENTS_DIR, agentId + ".agent.md");

        if (!Files.exists(path)) {
            throw new RuntimeException("Agente nao encontrado: " + agentId);
        }

        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler agente: " + agentId, e);
        }
    }
}

