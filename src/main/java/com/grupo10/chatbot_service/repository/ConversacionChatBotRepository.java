package com.grupo10.chatbot_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.grupo10.chatbot_service.model.ConversacionChatBot;


public interface ConversacionChatBotRepository extends JpaRepository<ConversacionChatBot, Integer> {
    
}
