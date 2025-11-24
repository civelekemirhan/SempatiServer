package com.wexec.SempatiServer.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

// BURASI SWAGGER CONFİG , BURANIN YAPISI DEĞİŞMELİYDİ ÇÜNKÜ İKİ FARKLI ORTAMDA ÇALIŞABİLMESİ LAZIMDI

@Configuration
@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "Sempati Ekibi",
                        email = "info@sempati.com",
                        url = "https://sempati.com"
                ),
                description = "Sempati Uygulaması Backend API Dokümantasyonu",
                title = "Sempati API",
                version = "1.0"
        ),
        servers = {
                @Server(
                        description = "Local Ortam",
                        url = "http://localhost:8080"
                ),
                @Server(
                        description = "Prod Ortam (Azure)",
                        url = "https://sempati-backend.azurewebsites.net"
                )
        },
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT auth description",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}