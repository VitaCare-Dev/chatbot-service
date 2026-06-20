package com.grupo10.chatbot_service;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChatbotServiceApplicationTest {

    @Test
    void constructor_InstanciaClaseCorrectamente() {
        ChatbotServiceApplication application = new ChatbotServiceApplication();
        assertNotNull(application);
    }

    @Test
    void main_IniciaAplicacionSpring() {
        try (MockedStatic<SpringApplication> mockedSpringApplication = Mockito.mockStatic(SpringApplication.class)) {
            ChatbotServiceApplication.main(new String[] {});
            mockedSpringApplication
                    .verify(() -> SpringApplication.run(ChatbotServiceApplication.class, new String[] {}));
        }
    }
}