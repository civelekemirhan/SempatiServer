package com.wexec.SempatiServer.controller;

import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.PagedResponse;
import com.wexec.SempatiServer.dto.PetDto;
import com.wexec.SempatiServer.dto.PostDto;
import com.wexec.SempatiServer.dto.PetRequest;
import com.wexec.SempatiServer.service.PetService;
import com.wexec.SempatiServer.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenericResponse<PetDto> addPet(@ModelAttribute PetRequest request) {
        return petService.addPet(request);
    }

    @PatchMapping(value = "/{petId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenericResponse<PetDto> updatePet(@PathVariable Long petId,
            @ModelAttribute PetRequest request) {
        return petService.updatePet(petId, request);
    }

    @GetMapping("/{petId}")
    public GenericResponse<PetDto> getPetById(@PathVariable Long petId) {
        return petService.getPetById(petId);
    }

    @GetMapping("/{petId}/posts")
    public GenericResponse<PagedResponse<PostDto>> getPetPosts(
            @PathVariable Long petId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // PostService'e yazdığımız metodu çağırıyoruz
        return postService.getPostsByPetId(petId, page, size);
    }
}