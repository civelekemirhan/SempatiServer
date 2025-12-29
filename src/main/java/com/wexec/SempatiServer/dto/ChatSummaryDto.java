package com.wexec.SempatiServer.dto;

import com.wexec.SempatiServer.entity.ProfileIcon;
import com.wexec.SempatiServer.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatSummaryDto {
    private Long userId; // Konuşulan kişinin ID'si
    private String nickname; // Konuşulan kişinin adı
    private ProfileIcon profileIcon; // Konuşulan kişinin profil ikonu

    private String lastMessage; // Son mesajın içeriği (veya "📷 Fotoğraf")
    private MessageType type; // Mesaj tipi (TEXT, IMAGE, AUDIO)
    private LocalDateTime timestamp; // Son mesaj saati

    private Long unreadCount; // Okunmamış mesaj sayısı
}