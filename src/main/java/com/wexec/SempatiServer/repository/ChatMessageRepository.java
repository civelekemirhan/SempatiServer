package com.wexec.SempatiServer.repository;

import com.wexec.SempatiServer.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // İki kullanıcı arasındaki konuşma geçmişini getirir
    List<ChatMessage> findByChatIdOrderByTimestampAsc(String chatId);
}