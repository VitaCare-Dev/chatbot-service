package com.grupo10.chatbot_service.service;

import com.grupo10.chatbot_service.constant.Constants;
import com.grupo10.chatbot_service.model.ConversacionChatBot;
import com.grupo10.chatbot_service.repository.ConversacionChatBotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroqServiceTest {

    @Mock
    private ConversacionChatBotRepository conversacionRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GroqService groqService;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(groqService, "apiKey", "test-api-key-12345");
        ReflectionTestUtils.setField(groqService, "model", "llama3-8b-8192");

        ReflectionTestUtils.setField(groqService, "restTemplate", restTemplate);
    }

    @Test
    void init_LimpiaEspaciosEnVariables() {
        // Arrange: Simulamos que las variables del properties venían con espacios extra
        ReflectionTestUtils.setField(groqService, "apiKey", "  api-key-con-espacios  ");
        ReflectionTestUtils.setField(groqService, "model", "  modelo-espaciado  ");

        // Act
        groqService.init();

        // Assert: Verificamos que el trim() hizo su trabajo
        assertEquals("api-key-con-espacios", ReflectionTestUtils.getField(groqService, "apiKey"));
        assertEquals("modelo-espaciado", ReflectionTestUtils.getField(groqService, "model"));
    }

    @Test
    void init_ManejaValoresNulosSinRomperse() {
        // Arrange: Simulamos que el properties no trajo nada
        ReflectionTestUtils.setField(groqService, "apiKey", null);
        ReflectionTestUtils.setField(groqService, "model", null);

        // Act
        groqService.init(); // Si el código maneja mal los nulos, esto lanzaría NullPointerException

        // Assert
        assertNull(ReflectionTestUtils.getField(groqService, "apiKey"));
    }

    @Test
    void consultarIA_RespuestaSinChoices_RetornaErrorDeExtraccion() {
        // Arrange: Simulamos un JSON de respuesta completamente vacío
        Map<String, Object> mockResponseBody = new HashMap<>();
        ResponseEntity<Map<String, Object>> mockResponseEntity = new ResponseEntity<>(mockResponseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(mockResponseEntity);

        // Act
        String resultado = groqService.consultarIA(1, "Hola");

        // Assert
        assertEquals("No se pudo extraer la respuesta.", resultado);
    }

    @Test
    void consultarIA_RespuestaSinUsage_RetornaSoloTextoLimpio() {
        // Arrange: Simulamos un JSON que sí trae 'choices' pero le falta el nodo
        // 'usage'
        Map<String, Object> mockResponseBody = new HashMap<>();
        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> firstChoice = new HashMap<>();
        Map<String, String> message = new HashMap<>();

        message.put(Constants.CONTENT_KEY, "Respuesta perfecta pero sin métricas de uso");
        firstChoice.put("message", message);
        choices.add(firstChoice);
        mockResponseBody.put("choices", choices);
        // Omitimos intencionalmente el put("usage", ...)

        ResponseEntity<Map<String, Object>> mockResponseEntity = new ResponseEntity<>(mockResponseBody, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(mockResponseEntity);

        // Act
        String resultado = groqService.consultarIA(1, "Hola");

        // Assert: Al no tener 'usage', el textoConTokens debe ser igual al textoLimpio
        assertEquals("Respuesta perfecta pero sin métricas de uso", resultado);
        assertFalse(resultado.contains("Total de tokens:"));
    }

    @Test
    void consultarIA_FlujoExitoso() {
        Integer idUsuario = 1;
        String mensajeUsuario = "Tengo dolor de cabeza, ¿qué puede ser?";
        String textoLimpioSimulado = "Es importante descansar y mantenerse hidratado. Si persiste, consulte a un médico.";

        Map<String, Object> mockResponseBody = crearMockRespuestaGroq(textoLimpioSimulado, 15, 25, 40);
        ResponseEntity<Map<String, Object>> mockResponseEntity = new ResponseEntity<>(mockResponseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("https://api.groq.com/openai/v1/chat/completions"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class))).thenReturn(mockResponseEntity);

        String textoConTokensResultante = groqService.consultarIA(idUsuario, mensajeUsuario);

        assertTrue(textoConTokensResultante.contains(textoLimpioSimulado));
        assertTrue(textoConTokensResultante.contains("Tokens de entrada: 15"));
        assertTrue(textoConTokensResultante.contains("Total de tokens: 40"));

        ArgumentCaptor<ConversacionChatBot> captor = ArgumentCaptor.forClass(ConversacionChatBot.class);
        verify(conversacionRepository, times(1)).save(captor.capture());

        ConversacionChatBot historialGuardado = captor.getValue();
        assertEquals(idUsuario, historialGuardado.getIdUsuario());
        assertEquals(mensajeUsuario, historialGuardado.getMensajeUsuario());
        assertEquals(textoLimpioSimulado, historialGuardado.getRespuestaIA());
        assertEquals(40, historialGuardado.getTokensConsumidos());
        assertNotNull(historialGuardado.getFechaHora());
    }

    @Test
    void consultarIA_RateLimitExcedido_RetornaMensajeSaturado() {
        // 1. Arrange
        Integer idUsuario = 2;
        String mensajeUsuario = "Hola";

        // Construir los headers que simulan la respuesta de Groq pidiendo que esperemos
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", "1");

        // Crear la excepción específica 429 Too Many Requests
        HttpClientErrorException exception429 = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                headers,
                null,
                null);

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(exception429);

        // 2. Act
        String resultado = groqService.consultarIA(idUsuario, mensajeUsuario);

        // 3. Assert
        assertEquals("El servicio de IA está temporalmente saturado. Intenta de nuevo más tarde.", resultado);
        verify(restTemplate, times(3)).exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class));
        verify(conversacionRepository, never()).save(any(ConversacionChatBot.class));
    }

    @Test
    void consultarIA_ErrorGeneralAPI_RetornaMensajeError() {
        Integer idUsuario = 3;
        String mensajeUsuario = "Hola";

        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Error interno de Groq"));
        String resultado = groqService.consultarIA(idUsuario, mensajeUsuario);

        assertEquals("Ocurrió un error al consultar la IA.", resultado);
        verify(conversacionRepository, never()).save(any(ConversacionChatBot.class));
    }

    private Map<String, Object> crearMockRespuestaGroq(String contenido, int promptTokens, int completionTokens,
            int totalTokens) {
        Map<String, Object> responseBody = new HashMap<>();

        List<Map<String, Object>> choices = new ArrayList<>();
        Map<String, Object> firstChoice = new HashMap<>();
        Map<String, String> message = new HashMap<>();
        message.put(Constants.CONTENT_KEY, contenido);
        firstChoice.put("message", message);
        choices.add(firstChoice);
        responseBody.put("choices", choices);

        Map<String, Object> usage = new HashMap<>();
        usage.put("prompt_tokens", promptTokens);
        usage.put("completion_tokens", completionTokens);
        usage.put("total_tokens", totalTokens);
        responseBody.put("usage", usage);

        return responseBody;
    }

    @Test
    void init_ModeloNuloConApiKeyNoNula_DejaModeloNulo() {
        // Arrange: apiKey no nula pero model sí es nulo (rama "model == null" en línea
        // 54)
        ReflectionTestUtils.setField(groqService, "apiKey", "some-api-key");
        ReflectionTestUtils.setField(groqService, "model", null);

        // Act
        groqService.init();

        // Assert: model debe seguir siendo nulo (no lanza NullPointerException)
        assertNull(ReflectionTestUtils.getField(groqService, "model"));
    }

    @Test
    void consultarIA_BodyRespuestaNulo_RetornaErrorDeExtraccion() {
        // Arrange: el exchange devuelve una respuesta con body nulo (rama "responseBody
        // == null")
        ResponseEntity<Map<String, Object>> mockResponseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(mockResponseEntity);

        // Act
        String resultado = groqService.consultarIA(1, "Hola");

        // Assert
        assertEquals("No se pudo extraer la respuesta.", resultado);
    }

    @Test
    void consultarIA_RateLimitSinHeaders_Interrumpido_RetornaMensajeInterrupcion() {
        // Arrange: excepción 429 sin headers (responseHeaders == null en línea 177)
        HttpClientErrorException exception429 = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, null, null);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(exception429);

        // Interrumpir el hilo para que Thread.sleep (con espera=10s por defecto) falle
        // inmediatamente
        Thread.currentThread().interrupt();

        // Act
        String resultado = groqService.consultarIA(1, "Hola");

        // Assert
        assertEquals("La solicitud fue interrumpida.", resultado);

        // Limpiar estado de interrupción para no afectar otros tests
        Thread.interrupted();
    }

    @Test
    void consultarIA_RateLimitAgotado_RetornaMensajeSaturadoDesdeLineaFinal() {
        // Este test cubre la línea 104: el bucle for sale via break cuando
        // intento==MAX_RETRIES
        // y el flujo llega al return después del for.
        // Los intentos 1 y 2 llaman a esperarRateLimit con Retry-After:0 (sleep 0ms)
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", "0");

        HttpClientErrorException exception429 = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers, null, null);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(exception429);

        // Act
        String resultado = groqService.consultarIA(1, "Hola");

        // Assert: el mensaje retornado desde línea 104
        assertEquals("El servicio de IA está temporalmente saturado. Intenta de nuevo más tarde.", resultado);
        verify(restTemplate, times(3)).exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class));
        verify(conversacionRepository, never()).save(any(ConversacionChatBot.class));
    }

    @Test
    void consultarIA_RateLimitInterrumpido_RetornaMensajeInterrupcion() {
        // Arrange: Creamos un error 429 pero SIN el header "Retry-After"
        HttpClientErrorException exception429 = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", new HttpHeaders(), null, null);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenThrow(exception429);

        // ¡EL TRUCO!: Interrumpimos el hilo de prueba antes de ejecutar el servicio.
        // Cuando esperarRateLimit intente hacer Thread.sleep(10000), fallará
        // inmediatamente
        // lanzando InterruptedException, evitando que nuestro test se quede congelado
        // 10 segundos.
        Thread.currentThread().interrupt();

        // Act
        String resultado = groqService.consultarIA(1, "Hola");

        // Assert
        assertEquals("La solicitud fue interrumpida.", resultado);

        // Limpieza: Limpiamos el estado de interrupción del hilo para no afectar otros
        // tests
        Thread.interrupted();
    }
}