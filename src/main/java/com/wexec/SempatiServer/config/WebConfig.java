package com.wexec.SempatiServer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @SuppressWarnings("null")
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Tüm endpointlere izin ver
                .allowedOriginPatterns("*") // Kimden gelirse gelsin kabul et (Localhost, Azure, Mobil)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // Tüm HTTP metodlarına izin ver
                .allowedHeaders("*") // Tüm başlıklara (Authorization vs.) izin ver
                .allowCredentials(true); // Cookie veya Auth bilgilerine izin ver
    }
}