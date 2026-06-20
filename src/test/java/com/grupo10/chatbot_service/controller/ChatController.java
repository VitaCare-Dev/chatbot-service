package com.grupo10.chatbot_service.controller;

import com.grupo10.chatbot_service.service.GroqService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private GroqService groqService;

    @InjectMocks
    private ChatController chatController;

    @Test
    void procesarMensaje_RetornaRespuestaExitosa() {
        ChatController.ChatRequest request = new ChatController.ChatRequest();
        request.setIdUsuario(1);
        request.setMensaje("Hola");

        when(groqService.consultarIA(1, "Hola")).thenReturn("Respuesta simulada");

        ResponseEntity<ChatController.ChatResponse> responseEntity = chatController.procesarMensaje(request);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals("Respuesta simulada", responseEntity.getBody().getRespuesta());
    }

    @Test
    void chatRequest_GettersYSettersFuncionan() {
        ChatController.ChatRequest request = new ChatController.ChatRequest();
        request.setIdUsuario(5);
        request.setMensaje("Prueba");

        assertEquals(5, request.getIdUsuario());
        assertEquals("Prueba", request.getMensaje());
    }

    @Test
    void chatResponse_GettersYSettersFuncionan() {
        ChatController.ChatResponse response = new ChatController.ChatResponse("Inicial");
        assertEquals("Inicial", response.getRespuesta());

        response.setRespuesta("Modificado");
        assertEquals("Modificado", response.getRespuesta());
    }
}