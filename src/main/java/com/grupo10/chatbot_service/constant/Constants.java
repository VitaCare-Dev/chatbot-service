package com.grupo10.chatbot_service.constant;

/**
 * Constantes de la aplicación usadas para el parseo de respuestas de la API de Groq.
 *
 * <p>Centraliza las claves de los campos JSON para evitar literales duplicados
 * a lo largo del código.</p>
 */
public final class Constants {

    private Constants() {
        throw new UnsupportedOperationException("Esta es una clase de utilidades y no puede ser instanciada");
    }

    /** Clave del campo de contenido textual en los mensajes de la API. */
    public static final String CONTENT_KEY = "content";

    /** Clave para el texto limpio extraído de la respuesta de la IA. */
    public static final String DATOS_KEY = "textoLimpio";

    /** Clave para el total de tokens consumidos en la respuesta. */
    public static final String TOKENS_KEY = "totalTokens";

    /** Clave para el texto de respuesta enriquecido con la información de tokens. */
    public static final String TEXTO_CON_TOKENS_KEY = "textoConTokens";
}
