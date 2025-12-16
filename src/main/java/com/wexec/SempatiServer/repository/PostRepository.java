package com.wexec.SempatiServer.repository;

import com.wexec.SempatiServer.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

        // 1. Anasayfa Akışı (SEED ile Rastgele Sıralama)
        @Query(value = "SELECT * FROM posts p " +
                        "WHERE (CAST(:type AS text) IS NULL OR p.type = CAST(:type AS text)) " +
                        "ORDER BY md5(CAST(p.id AS text) || :seed)", countQuery = "SELECT count(*) FROM posts p WHERE (CAST(:type AS text) IS NULL OR p.type = CAST(:type AS text))", nativeQuery = true)
        Page<Post> findAllRandomlyWithSeed(@Param("seed") String seed,
                        @Param("type") String type,
                        Pageable pageable);

        // 2. Profil (Sayfalı) - Bir kullanıcının postlarını getirir
        Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

        // 2.1. Profil (Liste) - DTO dönüşümleri için liste hali
        List<Post> findAllByUserIdOrderByCreatedAtDesc(Long userId);

        // 3. Yakındakiler (Konum Bazlı - Haversine Formülü)
        @Query(value = "SELECT * FROM posts p WHERE " +
                        "(6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) * " +
                        "cos(radians(p.longitude) - radians(:lon)) + " +
                        "sin(radians(:lat)) * sin(radians(p.latitude)))) < :distance " +
                        "AND (CAST(:type AS text) IS NULL OR p.type = CAST(:type AS text)) " +
                        "ORDER BY p.created_at DESC", countQuery = "SELECT count(*) FROM posts p WHERE " +
                                        "(6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) * " +
                                        "cos(radians(p.longitude) - radians(:lon)) + " +
                                        "sin(radians(:lat)) * sin(radians(p.latitude)))) < :distance " +
                                        "AND (CAST(:type AS text) IS NULL OR p.type = CAST(:type AS text))", nativeQuery = true)
        Page<Post> findNearbyPosts(@Param("lat") double lat,
                        @Param("lon") double lon,
                        @Param("distance") double distance,
                        @Param("type") String type,
                        Pageable pageable);

        // 4. Pet Profilindeki Postlar (Bir hayvanın etiketlendiği postlar)
        Page<Post> findByTaggedPetsIdOrderByCreatedAtDesc(Long petId, Pageable pageable);
}