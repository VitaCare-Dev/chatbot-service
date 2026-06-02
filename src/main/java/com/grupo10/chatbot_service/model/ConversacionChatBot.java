package com.grupo10.chatbot_service.model;

import java.time.LocalDateTime;
import lombok.Data;



@Data
public class ConversacionChatBot {
    private int id;
    private int idUsuario;
    private LocalDateTime fechaHora;
    private String mensajeUsuario;
    private String respuestaIA;
    private int tokensConsumidos;
}
