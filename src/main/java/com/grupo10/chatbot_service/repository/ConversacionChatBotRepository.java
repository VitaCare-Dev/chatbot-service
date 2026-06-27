package com.grupo10.chatbot_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.grupo10.chatbot_service.model.ConversacionChatBot;


/**
 * Repositorio JPA para el acceso a datos de las conversaciones del chatbot.
 *
 * <p>Hereda las operaciones CRUD estándar de {@link JpaRepository}, permitiendo
 * guardar, consultar, actualizar y eliminar registros de {@link ConversacionChatBot}
 * sin necesidad de implementación adicional.</p>
 */
public interface ConversacionChatBotRepository extends JpaRepository<ConversacionChatBot, Integer> {

}
