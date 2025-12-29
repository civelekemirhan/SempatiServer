package com.wexec.SempatiServer.repository;

import com.wexec.SempatiServer.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Geçmiş Mesajlar
    Page<ChatMessage> findByChatIdOrderByTimestampDesc(String chatId, Pageable pageable);

    // Son Sohbetler Sorgusu (Mevcut olan)
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

    // --- YENİ EKLENENLER ---

    // 1. Okunmamış Mesaj Sayısı (Bana gelen ve okunmamış olanlar)
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    // 2. Mesajları Okundu İşaretle (Toplu Güncelleme)
    // SenderId: Karşı taraf, RecipientId: Ben.
    // Yani "Ondan bana gelen tüm okunmamışları okundu yap."
    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.senderId = :senderId AND m.recipientId = :recipientId AND m.isRead = false")
    void markMessagesAsRead(@Param("senderId") Long senderId, @Param("recipientId") Long recipientId);

}