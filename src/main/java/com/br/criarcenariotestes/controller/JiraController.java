package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.dto.JiraIssueAttachmentsResponse;
import com.br.criarcenariotestes.business.service.JiraService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jira/tasks")
@RequiredArgsConstructor
@Validated
public class JiraController {

    private static final String TASK_KEY_REGEX = "^[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)*-\\d+$";

    private final JiraService jiraService;

    @GetMapping("/{taskKey}/attachments")
    public JiraIssueAttachmentsResponse listarAnexos(
            @PathVariable
            @Pattern(regexp = TASK_KEY_REGEX, message = "taskKey invalida. Exemplo: EX-OP-1122")
            String taskKey
    ) {
        return jiraService.listarAnexos(taskKey);
    }

    @GetMapping("/{taskKey}/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> baixarAnexo(
            @PathVariable
            @Pattern(regexp = TASK_KEY_REGEX, message = "taskKey invalida. Exemplo: EX-OP-1122")
            String taskKey,
            @PathVariable String attachmentId
    ) {
        JiraService.DownloadedAttachment attachment = jiraService.baixarAnexo(taskKey, attachmentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.fileName() + "\"")
                .contentType(MediaType.parseMediaType(attachment.mimeType()))
                .body(attachment.content());
    }

    @GetMapping("/{taskKey}/attachments/download-all")
    public ResponseEntity<byte[]> baixarTodosAnexos(
            @PathVariable
            @Pattern(regexp = TASK_KEY_REGEX, message = "taskKey invalida. Exemplo: EX-OP-1122")
            String taskKey
    ) {
        JiraService.DownloadedAttachment attachment = jiraService.baixarTodosAnexosZip(taskKey);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.fileName() + "\"")
                .contentType(MediaType.parseMediaType(attachment.mimeType()))
                .body(attachment.content());
    }
}
