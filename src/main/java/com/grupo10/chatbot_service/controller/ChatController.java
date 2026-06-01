package com.grupo10.chatbot_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grupo10.chatbot_service.service.GroqService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final GroqService groqService;

    public ChatController(GroqService groqService) {
        this.groqService = groqService;
    }

    @PostMapping("/enviar")
    public ResponseEntity<ChatResponse> procesarMensaje(@RequestBody ChatRequest request) {
        String respuestaIA = groqService.consultarIA(request.getMensaje());
        return ResponseEntity.ok(new ChatResponse(respuestaIA));
    }

    static class ChatRequest {
        private String mensaje;

        public String getMensaje() {
            return mensaje;
        }

        public void setMensaje(String mensaje) {
            this.mensaje = mensaje;
        }
    }

    static class ChatResponse {
        private String respuesta;

        public ChatResponse(String respuesta) {
            this.respuesta = respuesta;
        }

        public String getRespuesta() {
            return respuesta;
        }

        public void setRespuesta(String respuesta) {
            this.respuesta = respuesta;
        }
    }

}
