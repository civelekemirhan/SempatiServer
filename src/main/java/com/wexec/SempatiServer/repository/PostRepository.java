package com.wexec.SempatiServer.repository;

import com.wexec.SempatiServer.entity.Post;
import com.wexec.SempatiServer.entity.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 1. Anasayfa Akışı (Rastgele + Filtreli)
    // JPQL'e geçtik: Dinamik filtreleme (:type IS NULL) için en temiz yöntem budur.
    @Query("SELECT p FROM Post p " +
            "WHERE p.id NOT IN :excludedPostIds " +
            "AND (:type IS NULL OR p.type = :type) " +
            "ORDER BY FUNCTION('RANDOM')")
    Page<Post> findAllRandomly(@Param("excludedPostIds") List<Long> excludedPostIds,
                               @Param("type") PostType type,
                               Pageable pageable);

    // 2. Bir Başkasının Profili (Değişiklik yok)
    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 3. Yakındaki Postlar (Konum + Filtreli)
    // Matematik formülünü JPQL formatına uyarladık ve Type filtresini ekledik.
    @Query("SELECT p FROM Post p WHERE " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) * " +
            "cos(radians(p.longitude) - radians(:lon)) + " +
            "sin(radians(:lat)) * sin(radians(p.latitude)))) < :distance " +
            "AND (:type IS NULL OR p.type = :type) " +
            "ORDER BY p.createdAt DESC")
    Page<Post> findNearbyPosts(@Param("lat") double lat,
                               @Param("lon") double lon,
                               @Param("distance") double distance,
                               @Param("type") PostType type,
                               Pageable pageable);

    // (Eski metod, artık kullanılmayabilir ama durmasında sakınca yok)
    List<Post> findByTypeOrderByCreatedAtDesc(PostType type);
}