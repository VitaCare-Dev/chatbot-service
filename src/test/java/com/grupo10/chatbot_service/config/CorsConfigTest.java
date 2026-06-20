package com.grupo10.chatbot_service.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorsConfigTest {

    @Mock
    private CorsRegistry corsRegistry;

    @Mock
    private CorsRegistration corsRegistration;

    @Test
    void corsConfigurer_DebeConfigurarMapeosCorrectamente() {
        // 1. Arrange: Preparar la configuración y simular el encadenamiento (Fluent
        // API)
        CorsConfig corsConfig = new CorsConfig();
        WebMvcConfigurer configurer = corsConfig.corsConfigurer();

        // Le enseñamos al mock cómo responder a la cadena de métodos para que no lance
        // NullPointerException
        when(corsRegistry.addMapping(anyString())).thenReturn(corsRegistration);
        when(corsRegistration.allowedOrigins(anyString())).thenReturn(corsRegistration);
        when(corsRegistration.allowedMethods(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(corsRegistration);
        when(corsRegistration.allowedHeaders(anyString())).thenReturn(corsRegistration);
        when(corsRegistration.allowCredentials(anyBoolean())).thenReturn(corsRegistration);

        // maxAge devuelve CorsRegistration, por lo que lo simulamos también
        when(corsRegistration.maxAge(anyLong())).thenReturn(corsRegistration);

        // 2. Act: Ejecutar el método interno addCorsMappings
        configurer.addCorsMappings(corsRegistry);

        // 3. Assert: Verificar que el Bean se crea y que las reglas exactas se
        // aplicaron
        assertNotNull(configurer);

        verify(corsRegistry, times(1)).addMapping("/api/**");
        verify(corsRegistration, times(1)).allowedOrigins("*");
        verify(corsRegistration, times(1)).allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
        verify(corsRegistration, times(1)).allowedHeaders("*");
        verify(corsRegistration, times(1)).allowCredentials(false);
        verify(corsRegistration, times(1)).maxAge(3600);
    }
}