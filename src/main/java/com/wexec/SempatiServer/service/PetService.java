package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.*;
import com.wexec.SempatiServer.dto.*;
import com.wexec.SempatiServer.entity.*;
import com.wexec.SempatiServer.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final S3Service s3Service;

    // YENİ: Görüntü Analiz Servisini Ekledik
    private final IImageAnalysisService imageAnalysisService;

    // Pet Ekleme
    public GenericResponse<PetDto> addPet(PetRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String photoUrl = null;

        // Fotoğraf yüklenmişse kontrol et ve yükle
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            validateAndProcessImage(request.getImage()); // Kontrol Metodu
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

        // Eğer yeni fotoğraf geldiyse: Kontrol et -> Yükle -> Güncelle
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            validateAndProcessImage(request.getImage()); // Kontrol Metodu
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

    // --- YARDIMCI METOT: GÖRÜNTÜ DOĞRULAMA ---
    private void validateAndProcessImage(MultipartFile file) {
        String contentType = file.getContentType();

        // Sadece resim dosyalarında AI kontrolü yapıyoruz.
        // Pet profiline video konulmaz varsayımıyla sadece image check yeterli.
        if (contentType != null && contentType.startsWith("image")) {
            try {
                // Eğer kedi/köpek yoksa BusinessException fırlatır ve işlem durur.
                // PostService'deki gibi "yutma/continue" yapmıyoruz, çünkü pet fotosu zorunlu
                // doğru olmalı.
                imageAnalysisService.validateImageContent(file);

            } catch (BusinessException e) {
                // Hata "Kedi Yok" hatasıysa kullanıcıya net mesaj gitsin
                if (e.getErrorCode() == ErrorCode.IMAGE_INVALID_CONTENT) {
                    throw new BusinessException(ErrorCode.IMAGE_INVALID_CONTENT,
                            "Pet profil fotoğrafında kedi veya köpek tespit edilemedi.");
                }
                throw e; // Diğer teknik hataları olduğu gibi fırlat
            }
        }
    }
}