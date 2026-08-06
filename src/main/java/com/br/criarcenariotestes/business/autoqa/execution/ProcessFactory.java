package com.br.criarcenariotestes.business.autoqa.execution;

import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Abstração injetável de criação de processos. A implementação de produção
 * ({@link Default}) é o único ponto autorizado deste pacote a chamar
 * {@code ProcessBuilder.start()} — {@link ProcessExecutionService} apenas
 * monta o {@code ProcessBuilder} (executable e arguments sempre separados,
 * sem shell) e delega a criação real a esta abstração. Testes injetam uma
 * implementação fake que nunca inicia um processo real do sistema operacional.
 */
public interface ProcessFactory {

    Process start(ProcessBuilder processBuilder) throws IOException;

    @Component
    class Default implements ProcessFactory {
        @Override
        public Process start(ProcessBuilder processBuilder) throws IOException {
            return processBuilder.start();
        }
    }
}
