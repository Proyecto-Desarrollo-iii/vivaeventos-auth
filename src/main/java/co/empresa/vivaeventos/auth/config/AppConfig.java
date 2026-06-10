package co.empresa.vivaeventos.auth.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * RestTemplate construido con RestTemplateBuilder para que Spring Boot aplique
     * la instrumentación de Micrometer Tracing y propague el contexto de traza
     * (cabeceras B3) a los servicios downstream.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
