package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.BusinessException;
import com.wexec.SempatiServer.common.ErrorCode;
import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.*;
import com.wexec.SempatiServer.entity.*;
import com.wexec.SempatiServer.repository.CommentRepository;
import com.wexec.SempatiServer.repository.PetRepository;
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
    private final IImageAnalysisService imageAnalysisService;
    private final PetRepository petRepository;

    @Transactional
    public GenericResponse<PostDto> createPost(PostRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Eğer post tipi LOST, CARETAKER veya ADOPTION ise konum ZORUNLU.
        if (request.getType() == PostType.LOST ||
                request.getType() == PostType.CARETAKER ||
                request.getType() == PostType.ADOPTION) {

            if (request.getLatitude() == null || request.getLongitude() == null ||
                    request.getLatitude() == 0.0 || request.getLongitude() == 0.0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST,
                        "Kayıp, Bakıcı ve Sahiplendirme ilanlarında konum bilgisi zorunludur.");
            }
        }

        List<String> mediaUrls = new ArrayList<>();
        boolean userSentFiles = request.getImages() != null && !request.getImages().isEmpty();

        if (userSentFiles) {
            for (MultipartFile file : request.getImages()) {
                if (file == null || file.isEmpty()) {
                    continue;
                }

                String contentType = file.getContentType();

                if (contentType != null && (contentType.startsWith("image") || contentType.startsWith("video"))) {
                    try {
                        imageAnalysisService.validateImageContent(file);
                        String url = s3Service.uploadFile(file);
                        mediaUrls.add(url);

                    } catch (BusinessException e) {
                        if (e.getErrorCode() == ErrorCode.IMAGE_INVALID_CONTENT) {
                            continue;
                        } else {
                            throw e;
                        }
                    }
                } else {
                    // Resim/Video dışındaki dosyalar direkt yüklensin
                    String url = s3Service.uploadFile(file);
                    mediaUrls.add(url);
                }
            }
        }

        // Medya Validasyonu
        if (userSentFiles && mediaUrls.isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_CONTENT,
                    "Yüklediğiniz dosyaların hiçbirinde kedi/köpek tespit edilemedi.");
        }

        // Eğer kullanıcı hiç dosya göndermediyse ve açıklama da yoksa hata ver
        if (mediaUrls.isEmpty() && (request.getDescription() == null || request.getDescription().trim().isEmpty())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Geçerli bir fotoğraf/video veya açıklama girmelisiniz.");
        }

        List<Pet> taggedPets = new ArrayList<>();
        if (request.getTaggedPetIds() != null && !request.getTaggedPetIds().isEmpty()) {
            taggedPets = petRepository.findAllById(request.getTaggedPetIds());
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
                .taggedPets(taggedPets)
                .build();

        Post savedPost = postRepository.save(post);
        return GenericResponse.success(convertToPostDto(savedPost));
    }

    // --- Yorum Ekleme ---
    public GenericResponse<CommentDto> addComment(Long postId, CommentRequest request) {
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
        return GenericResponse.success(convertToCommentDto(savedComment));
    }

    // toggle uygulamada işlevsellik sağladığından unlike için delete metodu
    // eklenmedi.
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
    // VERİ ÇEKME - SEED (TOHUM) MANTIĞI
    // =================================================================

    // 1. Tüm Postlar (Feed Akışı)
    @Transactional(readOnly = true)
    public GenericResponse<PagedResponse<PostDto>> getAllPosts(int page, int size, String seed, PostType type) {

        if (seed == null || seed.isEmpty()) {
            seed = "default-seed";
        }

        String typeStr = (type != null) ? type.name() : null;
        Pageable pageable = PageRequest.of(page, size);

        Page<Post> postsPage = postRepository.findAllRandomlyWithSeed(seed, typeStr, pageable);

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

        Page<PostDto> dtoPage = postsPage.map(this::convertToPostDto);
        return GenericResponse.success(mapToPagedResponse(dtoPage));
    }

    // 3. Yakındaki Postlar (Konum)
    public GenericResponse<PagedResponse<PostDto>> getNearbyPosts(double lat, double lon, double distanceKm,
            PostType type, int page, int size) {

        // Enum'ı String'e çeviriyoruz (Native Query için), eğer null ise null gider.
        String typeStr = (type != null) ? type.name() : null;

        Pageable pageable = PageRequest.of(page, size);

        // Repository sorgusunu çağır
        Page<Post> postsPage = postRepository.findNearbyPosts(lat, lon, distanceKm, typeStr, pageable);

        Page<PostDto> dtoPage = postsPage.map(this::convertToPostDto);
        return GenericResponse.success(mapToPagedResponse(dtoPage));
    }

    // 4. Pet Profilindeki Postlar (Bir hayvanın etiketlendiği postlar)
    public GenericResponse<PagedResponse<PostDto>> getPostsByPetId(Long petId, int page, int size) {
        if (petId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Pet ID gereklidir.");
        }
        if (!petRepository.existsById(petId)) {
            throw new BusinessException(ErrorCode.PET_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Post> postsPage = postRepository.findByTaggedPetsIdOrderByCreatedAtDesc(petId, pageable);

        Page<PostDto> dtoPage = postsPage.map(this::convertToPostDto);
        return GenericResponse.success(mapToPagedResponse(dtoPage));
    }

    // 5. Post Silme
    @Transactional
    public GenericResponse<String> deletePost(Long postId) {

        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (postId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Post ID boş olamaz.");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Bu postun sahibi siz değilsiniz.");
        }

        postRepository.delete(post);

        return GenericResponse.success("Post başarıyla silindi.");
    }

    // 6. Yorum Silme
    @Transactional
    public GenericResponse<String> deleteComment(Long commentId) {

        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (commentId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Yorum ID boş olamaz.");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Bu yoruma ait sahibi siz değilsiniz.");
        }

        commentRepository.delete(comment);

        return GenericResponse.success("Yorum başarıyla silindi.");
    }

    // --- DTO Dönüştürücü ---
    PostDto convertToPostDto(Post post) {
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

                .user(UserSummaryDto.fromEntity(post.getUser()))

                .comments(post.getComments() != null ? post.getComments().stream()
                        .map(c -> convertToCommentDto(c))
                        .collect(Collectors.toList()) : new ArrayList<>())

                .likes(post.getLikes() != null ? post.getLikes().stream()
                        .map(l -> LikeDto.builder()
                                .id(l.getId())
                                .user(UserSummaryDto.fromEntity(l.getUser()))
                                .build())
                        .collect(Collectors.toList()) : new ArrayList<>())

                .taggedPetIds(post.getTaggedPets() != null ? post.getTaggedPets().stream()
                        .map(Pet::getId)
                        .collect(Collectors.toList())
                        : new ArrayList<>())
                .build();
    }

    private CommentDto convertToCommentDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .createdAt(comment.getCreatedAt())
                .user(UserSummaryDto.fromEntity(comment.getUser()))
                .build();
    }

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