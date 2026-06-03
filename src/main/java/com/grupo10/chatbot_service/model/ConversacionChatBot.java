package com.grupo10.chatbot_service.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;

import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;



@Data
@Entity
@Table(name = "TB_CONVERSACION_CHATBOT")
public class ConversacionChatBot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_MENSAJE")
    private int id;

    @Column(name = "ID_USUARIO")
    private int idUsuario;

    @Column(name = "FECHA_HORA")
    private LocalDateTime fechaHora;

    @Column(name = "MENSAJE_USUARIO")
    private String mensajeUsuario;

    @Column(name = "RESPUESTA_IA")
    private String respuestaIA;

    @Column(name = "TOKENS_CONSUMIDOS")
    private int tokensConsumidos;
}
