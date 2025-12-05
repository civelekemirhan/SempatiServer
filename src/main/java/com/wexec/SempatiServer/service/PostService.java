package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.BusinessException;
import com.wexec.SempatiServer.common.ErrorCode;
import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.*; // DTO paketini import ettik
import com.wexec.SempatiServer.entity.Comment;
import com.wexec.SempatiServer.entity.Post;
import com.wexec.SempatiServer.entity.PostLike;
import com.wexec.SempatiServer.entity.User;
import com.wexec.SempatiServer.repository.CommentRepository;
import com.wexec.SempatiServer.repository.PostLikeRepository;
import com.wexec.SempatiServer.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final S3Service s3Service;
    private final ImageAnalysisService imageAnalysisService;

    // --- Post Oluşturma ---
    @Transactional
    public GenericResponse<Post> createPost(PostRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<String> mediaUrls = new ArrayList<>();

        if (request.getImages() != null) {
            for (MultipartFile file : request.getImages()) {
                if (file != null && !file.isEmpty()) {

                    // --- 1. DÜZELTME: VİDEO KORUMASI ---
                    String contentType = file.getContentType();

                    // Sadece dosya bir RESİM ise yapay zekaya sor.
                    // Video ise (veya tipi belirsizse) AI kontrolünü atla.
                    if (contentType != null && contentType.startsWith("image")) {
                        imageAnalysisService.validateImageContent(file);
                    }
                    // -----------------------------------

                    // S3 YÜKLEME (Video da olsa resim de olsa yüklenir)
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

        Post savedPost = postRepository.save(post);
        return GenericResponse.success(savedPost);
    }

    // --- Yorum Ekleme ---
    public GenericResponse<Comment> addComment(Long postId, CommentRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (postId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Post ID boş olamaz.");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        Comment comment = Comment.builder()
                .text(request.getText())
                .post(post)
                .user(user)
                .createdAt(LocalDateTime.now())
                .build();

        Comment savedComment = commentRepository.save(comment);
        return GenericResponse.success(savedComment);
    }

    // --- Beğeni (Like/Unlike) Mantığı ---
    @Transactional
    public GenericResponse<String> toggleLike(Long postId) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (postId == null || user.getId() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

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
    // VERİ ÇEKME (DTO DÖNÜŞÜMLÜ HALİ)
    // =================================================================

    // 1. Tüm Postlar (Feed Akışı)
    @Transactional(readOnly = true)
    public GenericResponse<PagedResponse<PostDto>> getAllPosts(int page, int size, List<Long> excludedPostIds) { // Dönüş Tipi: PostDto
        Pageable pageable = PageRequest.of(page, size);

        boolean isFilterApplied = excludedPostIds != null && !excludedPostIds.isEmpty();

        if (!isFilterApplied) {
            excludedPostIds = new ArrayList<>();
            excludedPostIds.add(0L);
        }

        Page<Post> postsPage = postRepository.findAllRandomly(excludedPostIds, pageable);

        if (!postsPage.hasContent() && isFilterApplied) {
            throw new BusinessException(ErrorCode.NO_MORE_POSTS);
        }

        // --- 2. DÜZELTME: ENTITY -> DTO ÇEVİRİMİ ---
        Page<PostDto> dtoPage = postsPage.map(this::convertToPostDto);

        return GenericResponse.success(mapToPagedResponse(dtoPage));
    }

    // 2. Kullanıcı Profili
    public GenericResponse<PagedResponse<PostDto>> getUserPosts(Long userId, int page, int size) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "User ID gereklidir.");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postsPage = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        // Entity -> DTO Çevirimi
        Page<PostDto> dtoPage = postsPage.map(this::convertToPostDto);

        return GenericResponse.success(mapToPagedResponse(dtoPage));
    }

    // 3. Yakındaki Postlar
    public GenericResponse<PagedResponse<PostDto>> getNearbyPosts(double lat, double lon, double distanceKm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postsPage = postRepository.findNearbyPosts(lat, lon, distanceKm, pageable);

        // Entity -> DTO Çevirimi
        Page<PostDto> dtoPage = postsPage.map(this::convertToPostDto);

        return GenericResponse.success(mapToPagedResponse(dtoPage));
    }

    // --- Entity'den DTO'ya Çeviren Metod (Temizleyici) ---
    private PostDto convertToPostDto(Post post) {
        return PostDto.builder()
                .id(post.getId())
                .description(post.getDescription())
                .type(post.getType())
                .createdAt(post.getCreatedAt())
                .latitude(post.getLatitude())
                .longitude(post.getLongitude())
                .address(post.getAddress())
                .mediaUrls(post.getMediaUrls())
                .likeCount(post.getLikes() != null ? post.getLikes().size() : 0)

                // Kullanıcıyı DTO olarak al (Şifre, token vs. gelmez)
                .user(UserProfileResponse.fromEntity(post.getUser()))

                // Yorumları DTO'ya çevir
                .comments(post.getComments() != null ? post.getComments().stream()
                        .map(c -> CommentDto.builder()
                                .id(c.getId())
                                .text(c.getText())
                                .createdAt(c.getCreatedAt())
                                .user(UserProfileResponse.fromEntity(c.getUser()))
                                .build())
                        .collect(Collectors.toList()) : new ArrayList<>())

                // Beğenileri DTO'ya çevir
                .likes(post.getLikes() != null ? post.getLikes().stream()
                        .map(l -> LikeDto.builder()
                                .id(l.getId())
                                .user(UserProfileResponse.fromEntity(l.getUser()))
                                .build())
                        .collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }

    // --- Page -> PagedResponse Çevirici ---
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