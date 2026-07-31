package com.br.criarcenariotestes.business.autoqa.framework;

import com.br.criarcenariotestes.business.autoqa.exception.UnsupportedFrameworkException;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolve o adapter correto para o framework informado.
 * Frameworks não suportados lançam UnsupportedFrameworkException.
 */
@Component
@RequiredArgsConstructor
public class AutomationFrameworkResolver {

    private static final List<AutomationFramework> SUPPORTED =
            List.of(AutomationFramework.PLAYWRIGHT, AutomationFramework.CYPRESS);

    private final List<AutomationFrameworkAdapter> adapters;

    public AutomationFrameworkAdapter resolve(AutomationFramework framework) {
        if (framework == null) {
            throw new IllegalArgumentException("Framework não pode ser nulo");
        }
        return adapters.stream()
                .filter(a -> a.getFramework() == framework)
                .findFirst()
                .orElseThrow(() -> UnsupportedFrameworkException.notSupported(framework));
    }
}
