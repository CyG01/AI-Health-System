package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.example.properties.CorsProperties;

@Configuration
public class CorsConfig {

    private final CorsProperties corsProperties;

    public CorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        if (corsProperties.getAllowedOrigins() != null) {
            corsProperties.getAllowedOrigins().forEach(config::addAllowedOriginPattern);
        }
        if (corsProperties.getAllowedMethods() != null) {
            corsProperties.getAllowedMethods().forEach(config::addAllowedMethod);
        }
        if (corsProperties.getAllowedHeaders() != null) {
            if (corsProperties.getAllowedHeaders().size() == 1
                    && "*".equals(corsProperties.getAllowedHeaders().get(0))) {
                config.addAllowedHeader("*");
            } else {
                corsProperties.getAllowedHeaders().forEach(config::addAllowedHeader);
            }
        }
        if (Boolean.TRUE.equals(corsProperties.getAllowCredentials())) {
            config.setAllowCredentials(true);
        }
        if (corsProperties.getMaxAge() != null) {
            config.setMaxAge(corsProperties.getMaxAge());
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
