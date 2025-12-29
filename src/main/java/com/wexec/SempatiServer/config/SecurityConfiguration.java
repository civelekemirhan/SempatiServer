package com.wexec.SempatiServer.config;

import com.wexec.SempatiServer.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                        .cors(cors -> cors.configure(http))
                        .csrf(AbstractHttpConfigurer::disable)
                        .authorizeHttpRequests(auth -> auth
                                // 1. WebSocket Endpointine İzin Ver (Handshake için şart!)
                                .requestMatchers("/ws/**").permitAll() // <--- BURAYI EKLE

                                // Auth işlemleri
                                .requestMatchers(
                                        "/api/v1/auth/register",
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/verify",
                                        "/api/v1/auth/refresh-token"
                                ).permitAll()

                                // Şifre Sıfırlama İşlemleri
                                .requestMatchers(
                                        "/api/v1/auth/forgot-password",
                                        "/api/v1/auth/verify-reset-code",
                                        "/api/v1/auth/reset-password")
                                .permitAll()

                                // Swagger
                                .requestMatchers(
                                        "/v2/api-docs",
                                        "/v3/api-docs",
                                        "/v3/api-docs/**",
                                        "/swagger-resources/**",
                                        "/swagger-ui/**",
                                        "/webjars/**",
                                        "/swagger-ui.html")
                                .permitAll()

                                // Geri kalan her şey için Auth zorunlu
                                .anyRequest().authenticated())
                        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                        .authenticationProvider(authenticationProvider)
                        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}