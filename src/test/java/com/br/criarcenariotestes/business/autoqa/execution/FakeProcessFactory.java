package com.br.criarcenariotestes.business.autoqa.execution;

import java.io.IOException;

/** ProcessFactory de teste — nunca chama ProcessBuilder.start() de verdade. */
public class FakeProcessFactory implements ProcessFactory {

    private final Process process;
    private final IOException startFailure;
    private ProcessBuilder lastProcessBuilder;

    public FakeProcessFactory(Process process) {
        this.process = process;
        this.startFailure = null;
    }

    private FakeProcessFactory(IOException startFailure) {
        this.process = null;
        this.startFailure = startFailure;
    }

    public static FakeProcessFactory failingToStart(IOException failure) {
        return new FakeProcessFactory(failure);
    }

    @Override
    public Process start(ProcessBuilder processBuilder) throws IOException {
        this.lastProcessBuilder = processBuilder;
        if (startFailure != null) {
            throw startFailure;
        }
        return process;
    }

    public ProcessBuilder lastProcessBuilder() {
        return lastProcessBuilder;
    }
}
