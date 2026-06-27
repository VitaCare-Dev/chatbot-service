package com.grupo10.chatbot_service.dto;

import lombok.Data;

/**
 * DTO para la transferencia de datos del historial de conversaciones del chatbot.
 *
 * <p>Representa un registro resumido de una interacción entre el usuario y la IA,
 * incluyendo el mensaje enviado, la respuesta recibida y el consumo de tokens.</p>
 */
@Data
public class HistorialChatDto {

    /** Fecha y hora en que ocurrió la conversación, en formato ISO-8601. */
    private String fechaHora;

    /** Mensaje original enviado por el usuario. */
    private String mensajeUsuario;

    /** Respuesta generada por la IA. */
    private String respuestaIA;

    /** Número total de tokens consumidos en la interacción. */
    private int tokensConsumidos;

}
