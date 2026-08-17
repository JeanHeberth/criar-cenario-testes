package com.br.criarcenariotestes.business.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "zephyr")
public class ZephyrProperties {

    private boolean enabled = false;
    private String baseUrl = "https://api.zephyrscale.smartbear.com/v2";
    private String apiToken;
    private String projectKey;
    private String defaultStatusName = "Draft";
    private String defaultPriorityName = "Normal";
    // Status da execução criada ao adicionar o caso ao ciclo. "Not Executed"
    // é o default do Zephyr e o correto semanticamente: o cenário acabou de
    // ser gerado, nunca foi rodado. Obrigatório na API (POST /testexecutions
    // rejeita com 400 "statusName: must not be null" se ausente).
    private String defaultExecutionStatusName = "Not Executed";
    // Pasta raiz padrão (stack de automação) usada quando o pedido não
    // informa CenarioRequest#pastaRaiz. Vazio = casos criados direto na raiz
    // do projeto, como era antes.
    private String rootFolder = "";
}
