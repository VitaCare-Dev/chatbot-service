package com.grupo10.chatbot_service.constant;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstantsTest {

    @Test
    void constructorPrivado_LanzaExcepcion() throws NoSuchMethodException {
        Constructor<Constants> constructor = Constants.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);

        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
        assertEquals("Esta es una clase de utilidades y no puede ser instanciada", exception.getCause().getMessage());
    }

    @Test
    void constantes_TienenValoresCorrectos() {
        assertEquals("content", Constants.CONTENT_KEY);
        assertEquals("textoLimpio", Constants.DATOS_KEY);
        assertEquals("totalTokens", Constants.TOKENS_KEY);
        assertEquals("textoConTokens", Constants.TEXTO_CON_TOKENS_KEY);
    }
}