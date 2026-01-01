package com.wexec.SempatiServer.repository;

import com.wexec.SempatiServer.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    // Ben bu kişiyi daha önce engellemiş miyim?
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    // Engeli kaldırmak için kaydı bul
    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}