package com.br.criarcenariotestes.business.autoqa.model.execution;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Especificação imutável de um comando a ser executado. executable e
 * arguments são sempre separados — nunca uma string de comando completa.
 * workingDirectoryReference é uma referência lógica/sanitizada (nunca o path
 * absoluto do projeto real). environment nunca contém valores sensíveis:
 * qualquer chave cujo nome pareça segredo é rejeitada estruturalmente aqui,
 * como última linha de defesa independente de quem montou o mapa.
 */
public record CommandSpecification(
        ExecutionCommandId commandId,
        String executable,
        List<String> arguments,
        String workingDirectoryReference,
        Duration timeout,
        Map<String, String> environment,
        ExecutionCommandType type
) {
    private static final Pattern SENSITIVE_KEY = Pattern.compile(
            "(?i)(key|token|secret|password|credential|private|auth)");

    public CommandSpecification {
        Objects.requireNonNull(commandId, "commandId must not be null");
        Objects.requireNonNull(executable, "executable must not be null");
        executable = executable.trim();
        if (executable.isEmpty()) {
            throw new IllegalArgumentException("executable must not be blank");
        }
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
        workingDirectoryReference = workingDirectoryReference == null ? null : workingDirectoryReference.trim();
        Objects.requireNonNull(timeout, "timeout must not be null");
        environment = environment == null ? Map.of() : Map.copyOf(environment);
        for (String key : environment.keySet()) {
            if (SENSITIVE_KEY.matcher(key).find()) {
                throw new IllegalArgumentException("environment não pode conter variável sensível: " + key);
            }
        }
        Objects.requireNonNull(type, "type must not be null");
    }
}
