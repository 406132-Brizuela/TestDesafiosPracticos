package com.tp.desafiospracticos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS solo para desarrollo local del front en localhost:4200 (Angular dev server
 * pegándole directo al backend sin Gateway de por medio). Detrás de Gateway el
 * origen del browser nunca es el microservicio, así que esto no debe aplicar ahí
 * — de ahí el {@code @Profile("local")}: antes no tenía guarda de perfil y corría
 * siempre.
 */
@Configuration
@Profile("local")
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:4200")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
            }
        };
    }
}
