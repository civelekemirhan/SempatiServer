package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.*;
import com.wexec.SempatiServer.dto.*;
import com.wexec.SempatiServer.entity.*;
import com.wexec.SempatiServer.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final S3Service s3Service;

    // Pet Ekleme
    public GenericResponse<PetDto> addPet(PetRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String photoUrl = null;
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            photoUrl = s3Service.uploadFile(request.getImage());
        }

        Pet pet = Pet.builder()
                .name(request.getName())
                .genus(request.getGenus())
                .breed(request.getBreed())
                .age(request.getAge())
                .isNeutered(request.getIsNeutered() != null ? request.getIsNeutered() : false)
                .profilePictureUrl(photoUrl)
                .owner(user)
                .build();

        petRepository.save(pet);
        return GenericResponse.success(PetDto.fromEntity(pet));
    }

    // Pet Güncelleme
    @Transactional
    public GenericResponse<PetDto> updatePet(Long petId, PetRequest request) {

        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Pet bulunamadı."));

        if (!pet.getOwner().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Bu petin sahibi siz değilsiniz.");
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String photoUrl = s3Service.uploadFile(request.getImage());
            pet.setProfilePictureUrl(photoUrl);
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            pet.setName(request.getName());
        }
        if (request.getGenus() != null && !request.getGenus().trim().isEmpty()) {
            pet.setGenus(request.getGenus());
        }
        if (request.getBreed() != null && !request.getBreed().trim().isEmpty()) {
            pet.setBreed(request.getBreed());
        }
        if (request.getAge() != null) {
            pet.setAge(request.getAge());
        }
        if (request.getIsNeutered() != null) { 
        pet.setNeutered(request.getIsNeutered()); 
    }

        petRepository.save(pet);
        return GenericResponse.success(PetDto.fromEntity(pet));
    }
     

    // ID ile Pet Getir (Detay Sayfası İçin)
    public GenericResponse<PetDto> getPetById(Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Pet bulunamadı."));
        return GenericResponse.success(PetDto.fromEntity(pet));
    }
}