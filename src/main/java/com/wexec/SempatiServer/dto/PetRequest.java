package com.wexec.SempatiServer.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PetRequest {
    private String name;
    private String genus;
    private String breed;
    private Integer age;
    private boolean isNeutered;
    private MultipartFile image; // Profil fotosu (Zorunlu değilse null gelebilir)
}