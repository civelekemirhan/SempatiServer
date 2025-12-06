package com.wexec.SempatiServer.controller;

import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.PetDto;
import com.wexec.SempatiServer.dto.PetRequest;
import com.wexec.SempatiServer.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenericResponse<PetDto> addPet(@ModelAttribute PetRequest request) {
        return petService.addPet(request);
    }

    @GetMapping("/{petId}")
    public GenericResponse<PetDto> getPetById(@PathVariable Long petId) {
        return petService.getPetById(petId);
    }
}