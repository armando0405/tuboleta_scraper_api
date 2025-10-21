package com.armando0405.tuboletascraper.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Configuration
public class WebConfig {

    /**
     * 🔧 RestTemplate Bean - Para hacer peticiones HTTP
     *
     * Usado por:
     * - KeepAliveScheduler: Para hacer ping a /api/health
     * - Cualquier otro servicio que necesite HTTP client
     *
     * Configuración:
     * - Connection timeout: 5 segundos
     * - Read timeout: 5 segundos
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))  // Timeout para conectar
                .setReadTimeout(Duration.ofSeconds(5))     // Timeout para leer respuesta
                .build();
    }
}
