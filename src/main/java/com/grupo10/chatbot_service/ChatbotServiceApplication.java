package com.grupo10.chatbot_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada principal del microservicio de chatbot médico.
 *
 * <p>Inicializa el contexto de Spring Boot y arranca todos los componentes
 * de la aplicación, incluyendo la configuración de base de datos, CORS
 * y la integración con la API de Groq.</p>
 */
@SpringBootApplication
public class ChatbotServiceApplication {

	/**
	 * Inicia la aplicación Spring Boot.
	 *
	 * @param args argumentos de línea de comandos pasados al arranque
	 */
	public static void main(String[] args) {
		SpringApplication.run(ChatbotServiceApplication.class, args);
	}

}
