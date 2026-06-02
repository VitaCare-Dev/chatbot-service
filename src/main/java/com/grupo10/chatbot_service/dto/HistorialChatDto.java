package com.grupo10.chatbot_service.dto;

import lombok.Data;

@Data
public class HistorialChatDto {

    private String fechaHora;
    private String mensajeUsuario;
    private String respuestaIA;
    private int tokensConsumidos; 
    
}
