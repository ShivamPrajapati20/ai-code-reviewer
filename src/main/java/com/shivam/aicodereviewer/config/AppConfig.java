package com.shivam.aicoderereviewer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Value("${app.github.token}")
    private String githubToken;

    @Value("${app.claude.api-key}")
    private String claudeApiKey;

    @Bean(name = "githubWebClient")
    public WebClient githubWebClient() {
        return WebClient.builder()
            .baseUrl("https://api.github.com")
            .defaultHeader("Authorization",
                "Bearer " + githubToken)
            .defaultHeader("Accept",
                "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version",
                "2022-11-28")
            .build();
    }

    @Bean(name = "claudeWebClient")
    public WebClient claudeWebClient() {
        return WebClient.builder()
            .baseUrl("https://api.anthropic.com")
            .defaultHeader("x-api-key", claudeApiKey)
            .defaultHeader("anthropic-version",
                "2023-06-01")
            .defaultHeader("Content-Type",
                "application/json")
            .build();
    }
}