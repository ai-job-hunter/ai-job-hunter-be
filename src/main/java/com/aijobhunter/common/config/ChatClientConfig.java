package com.aijobhunter.common.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ChatClient 装配。
 *
 * <p>背景：spring-ai-starter-model-openai 会自动注册 {@code ChatClient.Builder}
 * ，但不会自动注册 {@code ChatClient} 本体。{@link com.aijobhunter.agent.BaseAgent}
 * 通过构造器注入 {@code ChatClient}，因此在这里显式 build 一次并暴露为 bean。
 *
 * <p>ObjectMapper 由 Spring Boot 4.x 的 {@code JacksonAutoConfiguration} 自动装配，
 * 类型为 {@code tools.jackson.databind.ObjectMapper}（Jackson 3）。
 */
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
