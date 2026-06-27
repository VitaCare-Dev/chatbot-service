package com.grupo10.chatbot_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grupo10.chatbot_service.service.GroqService;

/**
 * Controlador REST que expone los endpoints del chatbot médico.
 *
 * <p>Recibe los mensajes del usuario, los delega al servicio de IA y
 * devuelve la respuesta generada. Todos los endpoints están bajo la ruta base
 * {@code /api/chat}.</p>
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final GroqService groqService;

    /**
     * Crea el controlador inyectando el servicio de integración con Groq.
     *
     * @param groqService servicio encargado de consultar la API de IA
     */
    public ChatController(GroqService groqService) {
        this.groqService = groqService;
    }

    /**
     * Procesa un mensaje del usuario y devuelve la respuesta generada por la IA.
     *
     * <p>Endpoint: {@code POST /api/chat/enviar}</p>
     *
     * @param request cuerpo JSON con el identificador de usuario y el mensaje a enviar
     * @return {@code 200 OK} con la respuesta textual de la IA
     */
    @PostMapping("/enviar")
    public ResponseEntity<ChatResponse> procesarMensaje(@RequestBody ChatRequest request) {
        String respuestaIA = groqService.consultarIA(request.getIdUsuario(), request.getMensaje());
        return ResponseEntity.ok(new ChatResponse(respuestaIA));
    }

    /**
     * Modelo de entrada para el endpoint de chat.
     */
    static class ChatRequest {
        private Integer idUsuario;
        private String mensaje;

        /**
         * @return identificador del usuario que envía el mensaje
         */
        public Integer getIdUsuario() {
            return idUsuario;
        }

        /**
         * @param idUsuario identificador del usuario
         */
        public void setIdUsuario(Integer idUsuario) {
            this.idUsuario = idUsuario;
        }

        /**
         * @return texto del mensaje enviado por el usuario
         */
        public String getMensaje() {
            return mensaje;
        }

        /**
         * @param mensaje texto del mensaje
         */
        public void setMensaje(String mensaje) {
            this.mensaje = mensaje;
        }
    }

    /**
     * Modelo de salida para el endpoint de chat.
     */
    static class ChatResponse {
        private String respuesta;

        /**
         * Crea la respuesta con el texto generado por la IA.
         *
         * @param respuesta texto de la respuesta generada
         */
        public ChatResponse(String respuesta) {
            this.respuesta = respuesta;
        }

        /**
         * @return texto de la respuesta generada por la IA
         */
        public String getRespuesta() {
            return respuesta;
        }

        /**
         * @param respuesta texto de la respuesta
         */
        public void setRespuesta(String respuesta) {
            this.respuesta = respuesta;
        }
    }

}
