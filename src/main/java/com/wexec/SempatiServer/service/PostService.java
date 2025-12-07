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
import java.util.UUID;
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
    private final PetRepository petRepository;


    // --- Post Oluşturma ---
    @Transactional
    public GenericResponse<PostDto> createPost(PostRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        List<String> mediaUrls = new ArrayList<>();

        if (request.getImages() != null) {
            for (MultipartFile file : request.getImages()) {
                if (file != null && !file.isEmpty()) {
                    String contentType = file.getContentType();
                    if (contentType != null && contentType.startsWith("image")) {
                        try {
                            imageAnalysisService.validateImageContent(file);
                        } catch (BusinessException e) {
                            throw e;
                        }
                    }
                    String url = s3Service.uploadFile(file);
                    mediaUrls.add(url);
                }
            }
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
    private CommentDto convertToCommentDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .createdAt(comment.getCreatedAt())
                .user(UserSummaryDto.fromEntity(comment.getUser()))
                .build();
    }

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
    // VERİ ÇEKME - SEED (TOHUM) MANTIĞI
    // =================================================================

    // 1. Tüm Postlar (Feed Akışı)
    // List<Long> excludedPostIds YERİNE String seed kullanıyoruz.
    @Transactional(readOnly = true)
    public GenericResponse<PagedResponse<PostDto>> getAllPosts(int page, int size, String seed, PostType type) {

        if (seed == null || seed.isEmpty()) {
            seed = "default-seed";
        }

        // DÜZELTME BURASI:
        // Enum'ı String'e çeviriyoruz. Eğer null ise null string gider.
        String typeStr = (type != null) ? type.name() : null;

        Pageable pageable = PageRequest.of(page, size);

        // Repository'e String halini gönderiyoruz
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
    public GenericResponse<PagedResponse<PostDto>> getNearbyPosts(double lat, double lon, double distanceKm, PostType type, int page, int size) {

        // DÜZELTME BURASI:
        String typeStr = (type != null) ? type.name() : null;

        Pageable pageable = PageRequest.of(page, size);

        // Repository'e String halini gönderiyoruz
        Page<Post> postsPage = postRepository.findNearbyPosts(lat, lon, distanceKm, typeStr, pageable);

        Page<PostDto> dtoPage = postsPage.map(this::convertToPostDto);
        return GenericResponse.success(mapToPagedResponse(dtoPage));
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
                        .map(c -> CommentDto.builder()
                                .id(c.getId())
                                .text(c.getText())
                                .createdAt(c.getCreatedAt())
                                .user(UserSummaryDto.fromEntity(c.getUser()))
                                .build())
                        .collect(Collectors.toList()) : new ArrayList<>())

                .likes(post.getLikes() != null ? post.getLikes().stream()
                        .map(l -> LikeDto.builder()
                                .id(l.getId())
                                .user(UserSummaryDto.fromEntity(l.getUser()))
                                .build())
                        .collect(Collectors.toList()) : new ArrayList<>())

                .taggedPetIds(post.getTaggedPets() != null ?
                        post.getTaggedPets().stream()
                                .map(Pet::getId)
                                .collect(Collectors.toList())
                        : new ArrayList<>())
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