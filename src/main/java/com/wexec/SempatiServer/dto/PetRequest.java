package com.wexec.SempatiServer.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;

@Data
public class PetRequest {
    private String name;
    private String genus;
    private String breed;
    private String gender;
    private LocalDate birthDate;
    private MultipartFile image; // Profil fotosu
}