package com.edulearn.lesson.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * RestTemplate used to call course-service to update totalDuration
     * after any lesson add / delete / update.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
