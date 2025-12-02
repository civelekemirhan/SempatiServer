package com.wexec.SempatiServer.entity; // Düzeltildi

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_codes")
@Data
@NoArgsConstructor
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    private boolean used = false;

    private String resetToken;

    public VerificationCode(String code, User user, int expiryMinutes) {
        this.code = code;
        this.user = user;
        this.expiryDate = LocalDateTime.now().plusMinutes(expiryMinutes);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}