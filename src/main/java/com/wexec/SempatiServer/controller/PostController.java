package com.wexec.SempatiServer.controller;

import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.CommentRequest;
import com.wexec.SempatiServer.dto.PagedResponse; // <-- Yeni DTO
import com.wexec.SempatiServer.dto.PostRequest;
import com.wexec.SempatiServer.entity.Comment;
import com.wexec.SempatiServer.entity.Post;
import com.wexec.SempatiServer.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 1. Post Oluşturma (Aynen Kaldı)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenericResponse<Post> createPost(@ModelAttribute PostRequest request) {
        return postService.createPost(request);
    }

    // 2. Tüm Postları Listeleme (Feed Akışı - GÜNCELLENDİ)
    // Artık sayfa ve boyut alıyor. Örn: ?page=0&size=10
    @GetMapping
    public GenericResponse<PagedResponse<Post>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return postService.getAllPosts(page, size);
    }

    // 3. Yakınlardaki Postlar (Konum - GÜNCELLENDİ)
    // Hem konum hem sayfalama alıyor.
    @GetMapping("/nearby")
    public GenericResponse<PagedResponse<Post>> getNearbyPosts(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "10") double dist,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return postService.getNearbyPosts(lat, lon, dist, page, size);
    }

    // 4. Bir Kullanıcının Postları (Profil - YENİ EKLENDİ)
    // Başkasının profiline girince onun postlarını sayfalayarak getirir.
    @GetMapping("/user/{userId}")
    public GenericResponse<PagedResponse<Post>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return postService.getUserPosts(userId, page, size);
    }

    // 5. Yorum Yapma (Aynen Kaldı)
    @PostMapping("/{postId}/comments")
    public GenericResponse<Comment> addComment(
            @PathVariable Long postId,
            @RequestBody CommentRequest request) {
        return postService.addComment(postId, request);
    }

    // 6. Beğen / Vazgeç (Aynen Kaldı)
    @PostMapping("/{postId}/like")
    public GenericResponse<String> toggleLike(@PathVariable Long postId) {
        return postService.toggleLike(postId);
    }
}