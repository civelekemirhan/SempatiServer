package com.wexec.SempatiServer.repository; // Düzeltildi

import com.wexec.SempatiServer.entity.VerificationCode; // Düzeltildi
import com.wexec.SempatiServer.entity.User; // Düzeltildi
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findByUserAndUsedFalse(User user);

    Optional<VerificationCode> findByResetToken(String resetToken);

    Optional<VerificationCode> findByUser(User user);
}