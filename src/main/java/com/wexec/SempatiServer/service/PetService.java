package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.PetRequest;
import com.wexec.SempatiServer.entity.Pet;
import com.wexec.SempatiServer.entity.User;
import com.wexec.SempatiServer.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class PetService {

    private final PetRepository petRepository;
    private final S3Service s3Service;

    public GenericResponse<Pet> addPet(PetRequest request) {
        // Giriş yapmış kullanıcıyı al (JWT Filter sayesinde buraya User objesi gelir)
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String photoUrl = null;
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            photoUrl = s3Service.uploadFile(request.getImage());
        }

        Pet pet = Pet.builder()
                .name(request.getName())
                .genus(request.getGenus())
                .breed(request.getBreed())
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .profilePictureUrl(photoUrl)
                .owner(user)
                .build();

        petRepository.save(pet);
        return GenericResponse.success(pet);
    }
}