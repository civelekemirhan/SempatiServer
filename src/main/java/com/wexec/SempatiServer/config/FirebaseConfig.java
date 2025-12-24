package com.wexec.SempatiServer.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() {
        try {
            ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");

            // Dosya var mı kontrol et
            if (!resource.exists()) {
                System.out.println("UYARI: serviceAccountKey.json bulunamadı. Bildirimler çalışmayacak.");
                return null; // Dosya yoksa null dön, uygulama çökmesin
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            }
            return FirebaseApp.getInstance();

        } catch (IOException e) {
            System.err.println("Firebase başlatılamadı: " + e.getMessage());
            return null;
        }
    }
}