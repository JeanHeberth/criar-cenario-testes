package com.br.criarcenariotestes.business.autoqa.model.context;

import java.util.List;

/**
 * Comando permitido pela política de segurança do Auto QA.
 * A IA nunca retorna um comando livre — os comandos são montados
 * pelo framework adapter e validados pelo CommandPolicyService.
 */
public record AllowedCommand(

        String logicalName,

        String executable,

        List<String> args,

        String description

) {

    public static AllowedCommand of(String logicalName, String executable, List<String> args, String description) {
        return new AllowedCommand(logicalName, executable, args, description);
    }

    /**
     * Retorna a linha de comando completa para fins de log (nunca para execução via shell -c).
     */
    public String toLogLine() {
        String argsStr = args == null ? "" : String.join(" ", args);
        return executable + (argsStr.isBlank() ? "" : " " + argsStr);
    }
}
