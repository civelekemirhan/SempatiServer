package com.wexec.SempatiServer.controller;

import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.CommentDto;
import com.wexec.SempatiServer.dto.CommentRequest;
import com.wexec.SempatiServer.dto.PagedResponse;
import com.wexec.SempatiServer.dto.PostDto;
import com.wexec.SempatiServer.dto.PostRequest;
import com.wexec.SempatiServer.entity.PostType;
import com.wexec.SempatiServer.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 1. Post Oluşturma (Resim/Video Yüklemeli)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenericResponse<PostDto> createPost(@ModelAttribute PostRequest request) {
        return postService.createPost(request);
    }

    // 2. Anasayfa Akışı (Feed)
    // URL: GET /api/v1/posts?page=0&size=10&seed=123&type=LOST
    @GetMapping
    public GenericResponse<PagedResponse<PostDto>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String seed,
            @RequestParam(required = false) PostType type) {
        return postService.getAllPosts(page, size, seed, type);
    }

    // 3. Yakındaki Postları Getir
    // URL: GET /api/v1/posts/nearby?latitude=41.0&longitude=29.0&distance=2.0
    @GetMapping("/nearby")
    public GenericResponse<PagedResponse<PostDto>> getNearbyPosts(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false) PostType type,
            @RequestParam(defaultValue = "2.0") double distance, // Varsayılan 2 KM
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return postService.getNearbyPosts(latitude, longitude, distance, type, page, size);
    }

    // 4. Kullanıcı Postları
    @GetMapping("/user/{userId}")
    public GenericResponse<PagedResponse<PostDto>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return postService.getUserPosts(userId, page, size);
    }

    // 5. Yorum Ekleme
    @PostMapping("/{postId}/comments")
    public GenericResponse<CommentDto> addComment(
            @PathVariable Long postId,
            @RequestBody CommentRequest request) {
        return postService.addComment(postId, request);
    }

    @PostMapping("/{postId}/like")
    public GenericResponse<String> toggleLike(@PathVariable Long postId) {
        return postService.toggleLike(postId);
    }

    @DeleteMapping("/{postId}")
    public GenericResponse<String> deletePost(@PathVariable Long postId) {
        return postService.deletePost(postId);
    }

    @DeleteMapping("/comments/{commentId}")
    public GenericResponse<String> deleteComment(@PathVariable Long commentId) {
        return postService.deleteComment(commentId);
    }
}