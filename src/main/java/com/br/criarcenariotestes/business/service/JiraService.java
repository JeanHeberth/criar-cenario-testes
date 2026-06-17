package com.br.criarcenariotestes.business.service;

import com.br.criarcenariotestes.business.dto.JiraAttachmentResponse;
import com.br.criarcenariotestes.business.dto.JiraIssueAttachmentsResponse;
import com.br.criarcenariotestes.infrastructure.jira.JiraClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class JiraService {

    private final JiraClient jiraClient;

    public JiraIssueAttachmentsResponse listarAnexos(String taskKey) {
        JsonNode issue = jiraClient.buscarIssueComAnexos(taskKey);
        List<JiraAttachmentResponse> attachments = mapearAnexos(taskKey, issue);
        return new JiraIssueAttachmentsResponse(taskKey, attachments);
    }

    public DownloadedAttachment baixarAnexo(String taskKey, String attachmentId) {
        JsonNode issue = jiraClient.buscarIssueComAnexos(taskKey);
        JsonNode attachment = buscarAnexoPorId(issue, attachmentId);

        String fileName = attachment.path("filename").asText("arquivo.bin");
        String mimeType = attachment.path("mimeType").asText("application/octet-stream");
        String contentUrl = attachment.path("content").asText();

        if (contentUrl.isBlank()) {
            throw new ResponseStatusException(NOT_FOUND, "URL de conteudo do anexo nao encontrada no Jira");
        }

        byte[] fileBytes = jiraClient.baixarAnexo(contentUrl);
        return new DownloadedAttachment(fileName, mimeType, fileBytes);
    }

    public DownloadedAttachment baixarTodosAnexosZip(String taskKey) {
        JsonNode issue = jiraClient.buscarIssueComAnexos(taskKey);
        List<JsonNode> anexos = listarNosAnexo(issue);

        if (anexos.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Task sem anexos no Jira");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOutput = new ZipOutputStream(baos)) {

            Map<String, Integer> nomes = new HashMap<>();

            for (JsonNode anexo : anexos) {
                String nomeOriginal = anexo.path("filename").asText("arquivo.bin");
                String nomeSeguro = gerarNomeZipUnico(nomeOriginal, nomes);
                String contentUrl = anexo.path("content").asText();

                if (contentUrl.isBlank()) {
                    continue;
                }

                byte[] conteudo = jiraClient.baixarAnexo(contentUrl);
                ZipEntry entry = new ZipEntry(nomeSeguro);
                zipOutput.putNextEntry(entry);
                zipOutput.write(conteudo);
                zipOutput.closeEntry();
            }

            zipOutput.finish();

            return new DownloadedAttachment(
                    taskKey + ".zip",
                    "application/zip",
                    baos.toByteArray()
            );
        } catch (IOException e) {
            throw new RuntimeException("Erro ao compactar anexos da task Jira", e);
        }
    }

    private List<JiraAttachmentResponse> mapearAnexos(String taskKey, JsonNode issue) {
        List<JsonNode> anexos = listarNosAnexo(issue);

        List<JiraAttachmentResponse> mapped = new ArrayList<>();
        for (JsonNode attachment : anexos) {
            String id = attachment.path("id").asText();
            mapped.add(new JiraAttachmentResponse(
                    id,
                    attachment.path("filename").asText(),
                    attachment.path("mimeType").asText(),
                    attachment.path("size").asLong(0L),
                    "/jira/tasks/" + taskKey + "/attachments/" + id + "/download"
            ));
        }

        mapped.sort(Comparator.comparing(JiraAttachmentResponse::fileName, String.CASE_INSENSITIVE_ORDER));
        return mapped;
    }

    private JsonNode buscarAnexoPorId(JsonNode issue, String attachmentId) {
        List<JsonNode> anexos = listarNosAnexo(issue);

        if (anexos.isEmpty()) {
            throw new ResponseStatusException(NOT_FOUND, "Task sem anexos no Jira");
        }

        for (JsonNode attachment : anexos) {
            if (attachmentId.equals(attachment.path("id").asText())) {
                return attachment;
            }
        }

        throw new ResponseStatusException(NOT_FOUND, "Anexo nao encontrado na task informada");
    }

    private List<JsonNode> listarNosAnexo(JsonNode issue) {
        JsonNode attachmentsNode = issue.path("fields").path("attachment");
        if (!attachmentsNode.isArray()) {
            return List.of();
        }

        List<JsonNode> anexos = new ArrayList<>();
        for (JsonNode attachment : attachmentsNode) {
            anexos.add(attachment);
        }
        return anexos;
    }

    private String gerarNomeZipUnico(String fileName, Map<String, Integer> nomes) {
        String sanitizado = fileName.replace("\\", "_").replace("/", "_").trim();
        if (sanitizado.isBlank()) {
            sanitizado = "arquivo.bin";
        }

        int contador = nomes.getOrDefault(sanitizado, 0);
        nomes.put(sanitizado, contador + 1);

        if (contador == 0) {
            return sanitizado;
        }

        int ponto = sanitizado.lastIndexOf('.');
        if (ponto > 0) {
            return sanitizado.substring(0, ponto) + "_" + contador + sanitizado.substring(ponto);
        }

        return sanitizado + "_" + contador;
    }

    public record DownloadedAttachment(
            String fileName,
            String mimeType,
            byte[] content
    ) {
    }
}
