package com.wexec.SempatiServer.config;

import com.wexec.SempatiServer.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // HATA BURADAYDI: buraya "/user" yazmamalısın.
        // "/user" prefix'i, setUserDestinationPrefix tarafından yönetilir.
        // Buraya sadece public kanallar (topic) ve kişisel kanalların kökü (queue) yazılır.

        registry.enableSimpleBroker("/queue");

        // İstemci bu prefix ile sunucuya mesaj atar
        registry.setApplicationDestinationPrefixes("/app");

        // Kullanıcıya özel mesaj göndermek için prefix
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Android'in bağlanacağı ana URL: ws://localhost:8080/ws
        // Native kullandığın için withSockJS() SİLDİM.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    // --- EKSİK OLAN VE "ANONYMOUS" SORUNUNU ÇÖZEN KISIM ---
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // Sadece CONNECT (Bağlanma) isteği geldiğinde çalışır
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {

                    // Header'dan Token'ı al
                    String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                        String token = authorizationHeader.substring(7);

                        try {
                            // Token'dan email/username bilgisini çıkar
                            String userEmail = jwtService.extractUsername(token);

                            if (userEmail != null) {
                                // Kullanıcı detaylarını veritabanından çek
                                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                                // Token geçerli mi diye kontrol et
                                if (jwtService.isTokenValid(token, userDetails)) {

                                    // Güvenlik nesnesini oluştur
                                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                            userDetails, null, userDetails.getAuthorities());

                                    // WebSocket oturumuna kimliği yapıştır! (Artık Anonymous değil)
                                    accessor.setUser(authToken);

                                    log.info("✅ WebSocket Kimlik Doğrulandı: {}", userEmail);
                                }
                            }
                        } catch (Exception e) {
                            log.error("❌ WebSocket Token Hatası: {}", e.getMessage());
                        }
                    }
                }
                return message;
            }
        });
    }
}