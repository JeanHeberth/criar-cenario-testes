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
}
