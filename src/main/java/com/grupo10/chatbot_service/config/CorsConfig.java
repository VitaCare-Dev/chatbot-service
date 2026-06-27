package com.grupo10.chatbot_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de CORS (Cross-Origin Resource Sharing) para el microservicio.
 *
 * <p>Permite que clientes de cualquier origen accedan a los endpoints bajo
 * {@code /api/**}, habilitando los métodos HTTP estándar y cualquier cabecera.</p>
 */
@Configuration
public class CorsConfig {

    /**
     * Registra las reglas de CORS en el contexto de Spring MVC.
     *
     * <p>Las reglas aplican a todos los endpoints bajo {@code /api/**} y
     * permiten cualquier origen con un tiempo de caché de preflight de 3600 segundos.</p>
     *
     * @return configurador de CORS con las reglas definidas
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}
