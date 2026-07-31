package com.br.criarcenariotestes.business.autoqa.model.request;

import com.br.criarcenariotestes.business.autoqa.model.context.FileToApply;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record FileApplicationRequest(
        @NotEmpty(message = "Files list cannot be empty")
        List<FileToApply> files
) {
}
