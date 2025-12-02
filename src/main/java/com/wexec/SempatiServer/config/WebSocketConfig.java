package com.wexec.SempatiServer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@SuppressWarnings("null")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // BURASI ANLIK MESAJLAŞMAYI SAĞLAMAK İÇİN GEREKLİDİR

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // İstemci (Android) bu prefix ile başlayan yerlere abone olur (Dinler)
        registry.enableSimpleBroker("/user");

        // İstemci bu prefix ile sunucuya mesaj atar
        registry.setApplicationDestinationPrefixes("/app");

        // Kullanıcıya özel mesaj göndermek için (user-specific) prefix
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Android'in bağlanacağı ana URL: ws://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // CORS sorunu olmaması için
                .withSockJS(); // WebSocket desteklemeyen tarayıcılar için fallback (Opsiyonel ama iyi)
    }
}