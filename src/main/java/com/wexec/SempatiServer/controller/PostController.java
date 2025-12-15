package com.wexec.SempatiServer.controller;

import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.*;
import com.wexec.SempatiServer.entity.PostType;
import com.wexec.SempatiServer.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 1. Post Oluşturma
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenericResponse<PostDto> createPost(@ModelAttribute PostRequest request) {
        return postService.createPost(request);
    }

    // 2. Tüm Postları Listeleme (Feed Akışı - SEED ile)
    // excludedPostIds YERİNE 'seed' geldi
    @GetMapping
    public GenericResponse<PagedResponse<PostDto>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String seed, // <-- YENİ PARAMETRE
            @RequestParam(required = false) PostType type) {

        return postService.getAllPosts(page, size, seed, type);
    }

    // 3. Yakınlardaki Postlar
    @GetMapping("/nearby")
    public GenericResponse<PagedResponse<PostDto>> getNearbyPosts(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "10") double dist,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) PostType type) {

        return postService.getNearbyPosts(lat, lon, dist, type, page, size);
    }

    // 4. Profil Postları
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

    // 6. Yorum Silme
    @DeleteMapping("/comments/{commentId}")
    public GenericResponse<String> deleteComment(@PathVariable Long commentId) {
    return postService.deleteComment(commentId);
    }

    // 7. Beğeni İşlemi
    @PostMapping("/{postId}/like")
    public GenericResponse<String> toggleLike(@PathVariable Long postId) {
        return postService.toggleLike(postId);
    }

    // 8. Post Silme
    @DeleteMapping("/{postId}")
    public GenericResponse<String> deletePost(@PathVariable Long postId) {
    return postService.deletePost(postId);
    }
    }

