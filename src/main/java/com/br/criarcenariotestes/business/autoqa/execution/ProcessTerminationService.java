package com.br.criarcenariotestes.business.autoqa.execution;

import com.br.criarcenariotestes.business.autoqa.execution.exception.ProcessTerminationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Responsável pelo encerramento normal, por timeout ou por cancelamento de
 * um processo: destroy() → aguarda um período curto → destroyForcibly() se
 * necessário → confirma término. Também tenta encerrar descendentes do
 * processo (via ProcessHandle, quando suportado) para não deixar
 * subprocessos órfãos. Nunca usa comandos do sistema para matar processos e
 * nunca encerra processos fora da árvore do processo informado.
 */
@Component
public class ProcessTerminationService {

    private static final Logger log = LoggerFactory.getLogger(ProcessTerminationService.class);

    private static final Duration DEFAULT_GRACE_PERIOD = Duration.ofSeconds(3);

    public TerminationOutcome terminate(Process process) {
        return terminate(process, DEFAULT_GRACE_PERIOD);
    }

    TerminationOutcome terminate(Process process, Duration gracePeriod) {
        if (!process.isAlive()) {
            return new TerminationOutcome(true, false);
        }

        process.destroy();
        boolean exitedGracefully = waitQuietly(process, gracePeriod);

        boolean forced = false;
        if (!exitedGracefully && process.isAlive()) {
            forced = true;
            process.destroyForcibly();
            destroyDescendantsForcibly(process);
            waitQuietly(process, gracePeriod);
        }

        boolean confirmed = !process.isAlive();
        if (!confirmed) {
            log.error("Process termination not confirmed. forced={}", forced);
            throw new ProcessTerminationException("Não foi possível confirmar o encerramento do processo");
        }
        return new TerminationOutcome(true, forced);
    }

    private void destroyDescendantsForcibly(Process process) {
        try {
            process.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
        } catch (UnsupportedOperationException ignored) {
            // pid/descendants indisponível para este processo; nada a fazer
        }
    }

    private boolean waitQuietly(Process process, Duration timeout) {
        try {
            return process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public record TerminationOutcome(boolean confirmed, boolean forced) {
    }
}
