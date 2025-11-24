package com.wexec.SempatiServer.repository;

import com.wexec.SempatiServer.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // Bir posta ait yorumları silmek vs. için gerekebilir
}