package com.wexec.SempatiServer.controller;

import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.CommentRequest;
import com.wexec.SempatiServer.dto.PagedResponse;
import com.wexec.SempatiServer.dto.PostDto;
import com.wexec.SempatiServer.dto.PostRequest;
import com.wexec.SempatiServer.entity.Comment;
import com.wexec.SempatiServer.entity.Post;
import com.wexec.SempatiServer.entity.PostType; // <--- BU IMPORT EKLENDİ
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

    // 1. Post Oluşturma (Aynen Kalıyor)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenericResponse<Post> createPost(@ModelAttribute PostRequest request) {
        return postService.createPost(request);
    }

    // 2. Tüm Postları Listeleme (Feed Akışı - GÜNCELLENDİ)
    // 'type' parametresi eklendi (LOST, NORMAL, ADOPTION vs.)
    @GetMapping
    public GenericResponse<PagedResponse<PostDto>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<Long> excludedPostIds,
            @RequestParam(required = false) PostType type) { // <--- EKLENDİ

        return postService.getAllPosts(page, size, excludedPostIds, type); // <--- SERVİSE GÖNDERİLDİ
    }

    // 3. Yakınlardaki Postlar (Konum - GÜNCELLENDİ)
    // 'type' parametresi eklendi
    @GetMapping("/nearby")
    public GenericResponse<PagedResponse<PostDto>> getNearbyPosts(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "10") double dist,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) PostType type) { // <--- EKLENDİ

        return postService.getNearbyPosts(lat, lon, dist, type, page, size); // <--- SERVİSE GÖNDERİLDİ
    }

    // 4. Profil Postları (Aynen Kalıyor)
    @GetMapping("/user/{userId}")
    public GenericResponse<PagedResponse<PostDto>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return postService.getUserPosts(userId, page, size);
    }

    // 5. Yorum Ekleme (Aynen Kalıyor)
    @PostMapping("/{postId}/comments")
    public GenericResponse<Comment> addComment(
            @PathVariable Long postId,
            @RequestBody CommentRequest request) {
        return postService.addComment(postId, request);
    }

    // 6. Beğeni İşlemi (Aynen Kalıyor)
    @PostMapping("/{postId}/like")
    public GenericResponse<String> toggleLike(@PathVariable Long postId) {
        return postService.toggleLike(postId);
    }
}