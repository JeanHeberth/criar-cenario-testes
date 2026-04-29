package com.br.criarcenariotestes.business.ai;

import com.br.criarcenariotestes.business.ai.AiProvider;
import com.br.criarcenariotestes.business.properties.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiProviderResolver {

    private final List<AiProvider> providers;
    private final AiProperties aiProperties;

    public AiProvider getActiveProvider() {
        return getByName(aiProperties.getActiveProvider());
    }

    public AiProvider getFallbackProvider() {
        return getByName(aiProperties.getFallbackProvider());
    }

    public AiProvider getByName(String providerName) {
        return providers.stream()
                .filter(provider -> provider.getName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Provider de IA não encontrado: " + providerName
                ));
    }
}