package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.BusinessException; // Hata fırlatmak için
import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.CommentRequest;
import com.wexec.SempatiServer.dto.PagedResponse;
import com.wexec.SempatiServer.dto.PostRequest;
import com.wexec.SempatiServer.entity.Comment;
import com.wexec.SempatiServer.entity.Post;
import com.wexec.SempatiServer.entity.PostLike;
import com.wexec.SempatiServer.entity.User;
import com.wexec.SempatiServer.repository.CommentRepository;
import com.wexec.SempatiServer.repository.PostLikeRepository;
import com.wexec.SempatiServer.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final S3Service s3Service;
    private final ImageAnalysisService imageAnalysisService; // <-- YENİ: AI Servisi Eklendi

    // --- Post Oluşturma (YENİ MANTIK: Önce AI, Sonra S3) ---
    @Transactional
    public GenericResponse<Post> createPost(PostRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<String> mediaUrls = new ArrayList<>();

        if (request.getImages() != null) {
            for (MultipartFile file : request.getImages()) {
                if (!file.isEmpty()) {

                    // 1. ÖNCE YAPAY ZEKA KONTROLÜ YAP
                    // Eğer kedi/köpek değilse burada hata fırlatır, kod aşağı inmez.
                    // Böylece boşuna S3'e çöp yüklememiş oluruz.
                    imageAnalysisService.validateImageContent(file);

                    // 2. ONAYLANDIYSA S3'E YÜKLE
                    String url = s3Service.uploadFile(file);
                    mediaUrls.add(url);
                }
            }
        }

        Post post = Post.builder()
                .description(request.getDescription())
                .type(request.getType())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress())
                .mediaUrls(mediaUrls)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        postRepository.save(post);
        return GenericResponse.success(post);
    }

    // --- Yorum Ekleme ---
    public GenericResponse<Comment> addComment(Long postId, CommentRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post bulunamadı!"));

        Comment comment = Comment.builder()
                .text(request.getText())
                .post(post)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        commentRepository.save(comment);
        return GenericResponse.success(comment);
    }

    // --- Beğeni (Like/Unlike) Mantığı ---
    @Transactional
    public GenericResponse<String> toggleLike(Long postId) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post bulunamadı!"));

        Optional<PostLike> existingLike = postLikeRepository.findByUserIdAndPostId(user.getId(), postId);

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            return GenericResponse.success("Beğeni geri çekildi (Unlike)");
        } else {
            PostLike newLike = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();
            postLikeRepository.save(newLike);
            return GenericResponse.success("Post beğenildi (Like)");
        }
    }

    // =================================================================
    // PAGINATION (Sayfalama) İLE VERİ ÇEKME
    // =================================================================

    // 1. Tüm Postlar (Feed Akışı)
    public GenericResponse<PagedResponse<Post>> getAllPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postsPage = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        return GenericResponse.success(mapToPagedResponse(postsPage));
    }

    // 2. Kullanıcı Profili (O kişinin postları)
    public GenericResponse<PagedResponse<Post>> getUserPosts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postsPage = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return GenericResponse.success(mapToPagedResponse(postsPage));
    }

    // 3. Yakındaki Postlar (Konum)
    public GenericResponse<PagedResponse<Post>> getNearbyPosts(double lat, double lon, double distanceKm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postsPage = postRepository.findNearbyPosts(lat, lon, distanceKm, pageable);
        return GenericResponse.success(mapToPagedResponse(postsPage));
    }

    // --- Yardımcı Metod: Page objesini bizim temiz DTO'ya çevirir ---
    private <T> PagedResponse<T> mapToPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}