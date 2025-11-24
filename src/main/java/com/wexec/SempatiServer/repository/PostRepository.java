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

    // 1. Anasayfa Akışı (Tüm Postlar - Sayfalı)
    // List yerine Page dönüyor, Pageable alıyor
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 2. Bir Başkasının Profili (O kişinin postları - Sayfalı)
    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 3. Yakındaki Postlar (Sayfalı Native Query)
    // Native Query'de pagination için 'countQuery' yazmak ZORUNLUDUR.
    // Yoksa Spring "Toplam kaç sayfa var?" sorusunun cevabını hesaplayamaz ve hata verir.
    @Query(value = "SELECT * FROM posts p WHERE " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) * " +
            "cos(radians(p.longitude) - radians(:lon)) + " +
            "sin(radians(:lat)) * sin(radians(p.latitude)))) < :distance " +
            "ORDER BY p.created_at DESC",

            countQuery = "SELECT count(*) FROM posts p WHERE " +
                    "(6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) * " +
                    "cos(radians(p.longitude) - radians(:lon)) + " +
                    "sin(radians(:lat)) * sin(radians(p.latitude)))) < :distance",

            nativeQuery = true)
    Page<Post> findNearbyPosts(@Param("lat") double lat,
                               @Param("lon") double lon,
                               @Param("distance") double distance,
                               Pageable pageable);

    // Bu metod filtreleme vb. için kullanılacaksa kalsın (Pagination gerekmiyorsa List kalabilir)
    List<Post> findByTypeOrderByCreatedAtDesc(PostType type);
}