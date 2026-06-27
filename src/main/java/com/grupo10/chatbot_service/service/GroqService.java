package com.grupo10.chatbot_service.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.grupo10.chatbot_service.constant.Constants;
import com.grupo10.chatbot_service.model.ConversacionChatBot;
import com.grupo10.chatbot_service.repository.ConversacionChatBotRepository;

import jakarta.annotation.PostConstruct;

/**
 * Servicio de integración con la API de Groq para el chatbot médico.
 *
 * <p>Gestiona el ciclo completo de una consulta: construye la petición HTTP,
 * la envía al modelo de IA, extrae la respuesta, persiste el historial en base
 * de datos y aplica reintentos automáticos ante límites de tasa (HTTP 429).</p>
 *
 * <p>El servicio actúa como asistente médico virtual y rechaza consultas fuera
 * del ámbito de la salud mediante el prompt de sistema configurado.</p>
 */
@Service
public class GroqService {

    private static final Logger logger = LoggerFactory.getLogger(GroqService.class);

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model}")
    private String model;

    /** Número máximo de reintentos ante errores de rate limit de la API. */
    private static final int MAX_RETRIES = 3;

    private final RestTemplate restTemplate;

    private final ConversacionChatBotRepository conversacionRepository;

    /**
     * Crea el servicio inyectando el repositorio de conversaciones.
     *
     * @param conversacionRepository repositorio JPA para persistir el historial de chat
     */
    public GroqService(ConversacionChatBotRepository conversacionRepository) {
        this.restTemplate = new RestTemplate();
        this.conversacionRepository = conversacionRepository;
    }

    /**
     * Limpia y registra en log los valores de configuración de la API al arranque.
     *
     * <p>Se ejecuta automáticamente tras la inyección de dependencias gracias a
     * {@code @PostConstruct}. Solo muestra los primeros 10 caracteres de la API key
     * para evitar exponer credenciales en los logs.</p>
     */
    @PostConstruct
    public void init() {
        if (apiKey != null) {
            apiKey = apiKey.trim();
            model = model != null ? model.trim() : model;
        }
        logger.info("=== GROQ Configuration ===");
        logger.info("API Key: {} (length: {})",
                apiKey != null ? (apiKey.substring(0, Math.min(10, apiKey.length())) + "...") : "NULL",
                apiKey != null ? apiKey.length() : 0);
        logger.info("Model: {}", model);
        logger.info("=========================");
    }

    /**
     * Envía el mensaje del usuario a la API de Groq y devuelve la respuesta de la IA.
     *
     * <p>En caso de recibir un error HTTP 429 (rate limit), reintenta hasta
     * {@value #MAX_RETRIES} veces respetando el tiempo indicado en la cabecera
     * {@code Retry-After}. Persiste cada interacción exitosa en la base de datos.</p>
     *
     * @param idUsuario     identificador del usuario que realiza la consulta
     * @param mensajeUsuario texto del mensaje enviado por el usuario
     * @return texto de respuesta de la IA con información de tokens de uso,
     *         o un mensaje de error descriptivo si la consulta falla
     */
    public String consultarIA(Integer idUsuario, String mensajeUsuario) {
        String url = "https://api.groq.com/openai/v1/chat/completions";
        HttpEntity<Map<String, Object>> requestEntity = buildRequest(mensajeUsuario);

        for (int intento = 1; intento <= MAX_RETRIES; intento++) {
            try {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url, HttpMethod.POST, requestEntity,
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        });

                Map<String, Object> datosExtraidos = extraerContenidoEstructurado(response.getBody());
                String textoLimpio = (String) datosExtraidos.get(Constants.DATOS_KEY);
                String textoConTokens = (String) datosExtraidos.get(Constants.TEXTO_CON_TOKENS_KEY);
                int totalTokens = (int) datosExtraidos.get(Constants.TOKENS_KEY);

                ConversacionChatBot historial = new ConversacionChatBot();
                historial.setIdUsuario(idUsuario);
                historial.setFechaHora(LocalDateTime.now());
                historial.setMensajeUsuario(mensajeUsuario);
                historial.setRespuestaIA(textoLimpio);
                historial.setTokensConsumidos(totalTokens);

                conversacionRepository.save(historial);

                return textoConTokens;

            } catch (HttpClientErrorException.TooManyRequests e) {
                if (intento < MAX_RETRIES && !esperarRateLimit(e, intento)) {
                    return "La solicitud fue interrumpida.";
                }
            } catch (Exception e) {
                logger.error("Error en la API: {}", e.getMessage(), e);
                return "Ocurrió un error al consultar la IA.";
            }
        }

        return "El servicio de IA está temporalmente saturado. Intenta de nuevo más tarde.";
    }

    /**
     * Construye la entidad HTTP con cabeceras y cuerpo para la petición a la API de Groq.
     *
     * <p>Incluye el prompt de sistema que restringe al asistente al ámbito médico
     * y el mensaje del usuario como rol {@code "user"}.</p>
     *
     * @param mensajeUsuario texto del mensaje enviado por el usuario
     * @return entidad HTTP lista para enviarse mediante {@link RestTemplate}
     */
    private HttpEntity<Map<String, Object>> buildRequest(String mensajeUsuario) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, String> mensajeSistema = new HashMap<>();
        mensajeSistema.put("role", "system");
        mensajeSistema.put(Constants.CONTENT_KEY, "Eres un asistente médico virtual. Tus reglas estrictas son: "
                + "1. Debes responder SIEMPRE en español. "
                + "2. Solo tienes permitido responder preguntas relacionadas con la salud, medicina o bienestar. "
                + "3. Si el usuario te hace una pregunta que no es de carácter médico, debes decirle amablemente que tu función es estrictamente médica y no puedes responder a su consulta.");

        Map<String, String> mensaje = new HashMap<>();
        mensaje.put("role", "user");
        mensaje.put(Constants.CONTENT_KEY, mensajeUsuario);

        List<Map<String, String>> listaMensajes = new ArrayList<>();
        listaMensajes.add(mensajeSistema);
        listaMensajes.add(mensaje);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", listaMensajes);

        return new HttpEntity<>(body, headers);
    }

    /**
     * Extrae y estructura el contenido relevante del cuerpo de respuesta de la API.
     *
     * <p>Recupera el texto generado por la IA desde {@code choices[0].message.content}
     * y, si está disponible, agrega al texto un resumen del uso de tokens
     * ({@code prompt_tokens}, {@code completion_tokens}, {@code total_tokens}).</p>
     *
     * @param responseBody mapa con la respuesta JSON deserializada de la API de Groq
     * @return mapa con las claves {@code textoLimpio}, {@code textoConTokens} y {@code totalTokens};
     *         en caso de respuesta nula o malformada devuelve valores por defecto
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extraerContenidoEstructurado(Map<?, ?> responseBody) {
        Map<String, Object> resultado = new HashMap<>();

        if (responseBody == null || !responseBody.containsKey("choices")) {
            resultado.put("textoLimpio", "No se pudo extraer la respuesta.");
            resultado.put("textoConTokens", "No se pudo extraer la respuesta.");
            resultado.put("totalTokens", 0);
            return resultado;
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
        Map<String, Object> firstChoice = choices.get(0);
        Map<String, String> message = (Map<String, String>) firstChoice.get("message");

        String textoLimpio = message.get(Constants.CONTENT_KEY);
        String textoConTokens = textoLimpio;
        int totalTokensInt = 0;

        if (responseBody.containsKey("usage")) {
            Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");

            String promptTokens = String.valueOf(usage.get("prompt_tokens"));
            String completionTokens = String.valueOf(usage.get("completion_tokens"));
            String totalTokens = String.valueOf(usage.get("total_tokens"));

            totalTokensInt = Integer.parseInt(totalTokens);

            textoConTokens += "\n\n--- Información de uso ---\n"
                    + "Tokens de entrada: " + promptTokens + "\n"
                    + "Tokens de salida: " + completionTokens + "\n"
                    + "Total de tokens: " + totalTokens;
        }

        resultado.put("textoLimpio", textoLimpio);
        resultado.put("textoConTokens", textoConTokens);
        resultado.put("totalTokens", totalTokensInt);

        return resultado;
    }

    /**
     * Pausa la ejecución del hilo el tiempo indicado por la API ante un error de rate limit.
     *
     * <p>Lee la cabecera {@code Retry-After} de la respuesta HTTP para determinar
     * cuántos segundos esperar. Si la cabecera no está presente, usa 10 segundos por defecto.</p>
     *
     * @param e       excepción HTTP 429 recibida de la API
     * @param intento número de intento actual (para logging)
     * @return {@code true} si la espera se completó satisfactoriamente;
     *         {@code false} si el hilo fue interrumpido durante la espera
     */
    private boolean esperarRateLimit(HttpClientErrorException.TooManyRequests e, int intento) {
        HttpHeaders responseHeaders = e.getResponseHeaders();
        String retryAfter = (responseHeaders != null) ? responseHeaders.getFirst("Retry-After") : null;
        long espera = (retryAfter != null) ? Long.parseLong(retryAfter) : 10L;

        logger.warn("Rate limit alcanzado. Reintento {}/{} en {}s", intento, MAX_RETRIES, espera);

        try {
            Thread.sleep(espera * 1000);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}