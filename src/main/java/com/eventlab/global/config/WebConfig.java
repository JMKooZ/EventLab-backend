package com.eventlab.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // application.yaml의 eventlab.cors.allowed-origins로 바꿀 수 있고, 기본값은 Nuxt 개발 서버
    @Value("${eventlab.cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                // JWT 적용 시 "Authorization" 추가
                .allowedHeaders("Content-Type", "X-Reconnect-Token")
                .maxAge(3600);
    }
}