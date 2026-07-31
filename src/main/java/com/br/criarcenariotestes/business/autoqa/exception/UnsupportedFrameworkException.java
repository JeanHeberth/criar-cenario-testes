package com.br.criarcenariotestes.business.autoqa.exception;

import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationLanguage;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lançada quando o framework informado não tem suporte implementado
 * ou é incompatível com a linguagem informada.
 */
public class UnsupportedFrameworkException extends ResponseStatusException {

    public UnsupportedFrameworkException(String reason) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, reason);
    }

    public static UnsupportedFrameworkException notSupported(AutomationFramework framework) {
        return new UnsupportedFrameworkException(
                "Framework não suportado nesta versão: " + framework.getDescricao()
                        + ". Frameworks disponíveis: PLAYWRIGHT, CYPRESS"
        );
    }

    public static UnsupportedFrameworkException incompatibleLanguage(
            AutomationFramework framework,
            AutomationLanguage language
    ) {
        return new UnsupportedFrameworkException(
                "A linguagem " + language.getDescricao()
                        + " não é compatível com o framework " + framework.getDescricao()
        );
    }

    public static UnsupportedFrameworkException divergence(
            AutomationFramework informed,
            AutomationFramework detected
    ) {
        return new UnsupportedFrameworkException(
                "O framework informado (" + informed.getDescricao()
                        + ") diverge do framework detectado no projeto ("
                        + detected.getDescricao()
                        + "). Corrija o framework informado ou verifique o projeto"
        );
    }
}
