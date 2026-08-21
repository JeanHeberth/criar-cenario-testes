package com.br.criarcenariotestes.controller;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.execution.ComandosPorFramework;
import com.br.criarcenariotestes.business.autoqa.model.scenario.AutomationType;
import com.br.criarcenariotestes.business.autoqa.model.scenario.CompatibilidadeFrameworkCanal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Publica a matriz framework → canais para o formulário montar os selects em
 * cascata (escolher Playwright oferece WEB_UI e API; REST Assured oferece só
 * API).
 *
 * Existe para a regra não ser duplicada: se o frontend tivesse a própria cópia,
 * as duas divergiriam no primeiro framework novo, e o usuário só descobriria ao
 * receber 400 numa combinação que o select ofereceu.
 *
 * Só metadado estático de domínio — nenhum dado de execução, projeto ou usuário.
 */
@RestController
@RequestMapping("/api/auto-qa/capabilities")
public class AutoQaCapabilitiesController {

    public record FrameworkOption(String framework, List<String> automationTypes, List<String> commands) {}

    @GetMapping("/frameworks")
    public List<FrameworkOption> frameworks() {
        return CompatibilidadeFrameworkCanal.frameworksSuportados().stream()
                .map(this::toOption)
                .toList();
    }

    private FrameworkOption toOption(AutomationFramework framework) {
        List<String> canais = CompatibilidadeFrameworkCanal.canaisDe(framework).stream()
                .map(AutomationType::name)
                .toList();
        return new FrameworkOption(framework.name(), canais, List.copyOf(ComandosPorFramework.de(framework)));
    }
}
