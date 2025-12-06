package com.wexec.SempatiServer.dto;

import com.wexec.SempatiServer.entity.Pet;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PetDto {
    private Long id;
    private String name;
    private String genus;
    private String breed;
    private Integer age;
    private boolean isNeutered;
    private String profilePictureUrl;

    public static PetDto fromEntity(Pet pet) {
        return PetDto.builder()
                .id(pet.getId())
                .name(pet.getName())
                .genus(pet.getGenus())
                .breed(pet.getBreed())
                .age(pet.getAge())
                .isNeutered(pet.isNeutered())
                .profilePictureUrl(pet.getProfilePictureUrl())
                .build();
    }
}