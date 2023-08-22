package com.gap.chatbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestTemplate;

@PropertySource("classpath:application.properties")
@Configuration
@Profile({ "local", "null", "default" })
public class LocalConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}



