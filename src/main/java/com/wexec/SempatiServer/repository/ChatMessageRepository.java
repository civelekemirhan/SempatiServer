package com.wexec.SempatiServer.repository;

import com.wexec.SempatiServer.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // chatId Entity'de String olduğu için parametre String olmalı
    Page<ChatMessage> findByChatIdOrderByTimestampDesc(String chatId, Pageable pageable);

    @Query(value = """
                SELECT m.* FROM chat_messages m
                INNER JOIN (
                    SELECT chat_id, MAX(timestamp) as max_date
                    FROM chat_messages
                    WHERE sender_id = :userId OR recipient_id = :userId
                    GROUP BY chat_id
                ) latest ON m.chat_id = latest.chat_id AND m.timestamp = latest.max_date
                ORDER BY m.timestamp DESC
            """, nativeQuery = true)
    List<ChatMessage> findRecentChats(@Param("userId") Long userId);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.senderId = :senderId AND m.recipientId = :recipientId AND m.isRead = false")
    void markMessagesAsRead(@Param("senderId") Long senderId, @Param("recipientId") Long recipientId);

    // --- SİLME METODLARI (String Parametre) ---

    // Yetki kontrolü için
    Optional<ChatMessage> findFirstByChatId(String chatId);

    // Silme işlemi için
    void deleteAllByChatId(String chatId);
}