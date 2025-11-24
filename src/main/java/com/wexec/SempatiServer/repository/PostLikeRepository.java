package com.wexec.SempatiServer.repository;

import com.wexec.SempatiServer.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    // Kullanıcı bu postu daha önce beğenmiş mi?
    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);

    // Beğeniyi geri çekmek için silme
    void deleteByUserIdAndPostId(Long userId, Long postId);
}