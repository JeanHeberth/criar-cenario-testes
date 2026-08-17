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
    // informa CenarioRequest#pastaDestino. Vazio = casos criados direto na
    // raiz do projeto, como era antes.
    private String rootFolder = "";

    /**
     * Se a publicação pode CRIAR pastas que ainda não existem no projeto.
     *
     * Com false, o gerador só deposita em pastas que o dono do board já
     * criou; um caminho inexistente falha dizendo qual era o esperado, em vez
     * de criar. Isso existe porque o estrago de errar é assimétrico e
     * permanente: a API do Zephyr não expõe remoção de pasta (DELETE
     * /folders responde 405), então cada pasta criada por engano vira
     * limpeza manual pela interface. Em time grande, onde cada squad traz sua
     * convenção de nome, criação livre multiplica variações da mesma pasta
     * ("Login", "Autenticação", "Auth") e quebra a deduplicação, que é
     * escopada por folderId.
     *
     * Default true para preservar o comportamento atual de quem já usa. Times
     * com taxonomia governada devem ligar em false.
     */
    private boolean allowFolderCreation = true;

    /** Ver FolderStrategyProperties. Desligada por padrão. */
    private FolderStrategyProperties folderStrategy = new FolderStrategyProperties();
}
