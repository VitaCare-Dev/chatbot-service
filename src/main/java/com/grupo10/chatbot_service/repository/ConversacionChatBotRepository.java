package com.grupo10.chatbot_service.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.grupo10.chatbot_service.model.ConversacionChatBot;

@Repository
public interface ConversacionChatBotRepository extends JpaRepository<ConversacionChatBot, Integer> {
    
}
