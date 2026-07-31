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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class AgentLoaderService {

    private static final Logger log = LoggerFactory.getLogger(AgentLoaderService.class);
    private static final String AGENTS_DIR = "agents";
    private static final String CLASSPATH_AGENTS_PATTERN = "classpath*:agents/**/*.agent.md";

    @Value("${agents.directory:}")
    private String configuredAgentsDir;

    public List<AgentInfoResponse> listAgents() {
        List<AgentInfoResponse> agents = new ArrayList<>();
        Path agentsPath = resolveAgentsDirectorySafely();

        if (agentsPath != null) {
            log.info("Listando agentes no diretório: {}", agentsPath.toAbsolutePath());
            try (Stream<Path> stream = Files.walk(agentsPath)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".agent.md"))
                        .forEach(entry -> {
                            String id = toAgentId(agentsPath.relativize(entry));
                            agents.add(new AgentInfoResponse(id, entry.getFileName().toString()));
                        });
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
            Path foundPath = findAgentInFileSystem(agentsPath, agentId);
            if (foundPath != null) {
                log.info("Carregando instruções do agente '{}'. path='{}'", agentId, foundPath.toAbsolutePath());
                try {
                    String conteudo = Files.readString(foundPath);
                    log.info("Agente '{}' carregado com sucesso. path='{}', length={}",
                            agentId,
                            foundPath.toAbsolutePath(),
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
                    String id = classpathAgentId(resource, fileName);
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
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String normalizedAgentId = normalizeAgentId(agentId);

        Resource direct = resolver.getResource("classpath:agents/" + normalizedAgentId + ".agent.md");
        if (direct.exists()) {
            return readClasspathResource(normalizedAgentId, direct);
        }

        try {
            Resource[] resources = resolver.getResources(CLASSPATH_AGENTS_PATTERN);
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (fileName == null || !fileName.endsWith(".agent.md")) {
                    continue;
                }
                String idFromResource = classpathAgentId(resource, fileName);
                if (normalizedAgentId.equals(idFromResource)
                        || fileName.equals(normalizedAgentId + ".agent.md")) {
                    return readClasspathResource(agentId, resource);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao procurar agente no classpath: " + agentId, e);
        }

        throw new RuntimeException("Agente nao encontrado: " + agentId);
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

    private Path findAgentInFileSystem(Path agentsRoot, String agentId) {
        String normalizedId = normalizeAgentId(agentId);
        List<Path> candidates = List.of(
                agentsRoot.resolve(normalizedId + ".agent.md"),
                agentsRoot.resolve(normalizedId).resolve(normalizedId.substring(normalizedId.lastIndexOf('/') + 1) + ".agent.md"),
                agentsRoot.resolve(normalizedId)
        );

        for (Path candidate : candidates) {
            Path normalized = candidate.normalize();
            if (Files.exists(normalized) && Files.isRegularFile(normalized)) {
                return normalized;
            }
        }

        String fileNameOnly = normalizedId.substring(normalizedId.lastIndexOf('/') + 1) + ".agent.md";
        try (Stream<Path> stream = Files.walk(agentsRoot)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(fileNameOnly))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao buscar agente no filesystem: " + agentId, e);
        }
    }

    private String classpathAgentId(Resource resource, String fileName) {
        try {
            String uri = resource.getURI().toString().replace("\\", "/");
            int index = uri.indexOf("/agents/");
            if (index >= 0) {
                String relative = uri.substring(index + "/agents/".length());
                if (relative.endsWith(".agent.md")) {
                    return relative.substring(0, relative.length() - ".agent.md".length());
                }
            }
        } catch (IOException ignored) {
            // fallback abaixo
        }
        return fileName.replace(".agent.md", "");
    }

    private String toAgentId(Path relativePath) {
        String normalized = relativePath.toString().replace("\\", "/");
        if (normalized.endsWith(".agent.md")) {
            return normalized.substring(0, normalized.length() - ".agent.md".length());
        }
        return normalized;
    }

    private String normalizeAgentId(String agentId) {
        String normalized = agentId.replace("\\", "/");
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith(".agent.md")) {
            normalized = normalized.substring(0, normalized.length() - ".agent.md".length());
        }
        return normalized;
    }

    private String readClasspathResource(String agentId, Resource resource) {
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
}
