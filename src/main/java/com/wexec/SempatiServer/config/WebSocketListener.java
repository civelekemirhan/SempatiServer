package com.wexec.SempatiServer.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class WebSocketListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketListener.class);

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        Principal user = event.getUser();

        if (user != null) {
            System.out.println("========================================");
            System.out.println("✅ YENİ SOCKET BAĞLANTISI!");
            System.out.println("👤 Kullanıcı (Principal) Adı: " + user.getName());
            System.out.println("========================================");
        } else {
            System.out.println("⚠️ İsimsiz (Anonymous) bir bağlantı gerçekleşti.");
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            System.out.println("❌ SOCKET BAĞLANTISI KOPTU: " + user.getName());
        }
    }
}