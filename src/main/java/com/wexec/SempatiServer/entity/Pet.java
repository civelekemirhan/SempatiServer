package com.wexec.SempatiServer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pets")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String genus;   // Kedi, Köpek vb.
    private String breed;   // Cins (Tekir, Golden)
    private String gender;  // Dişi, Erkek
    private LocalDate birthDate;

    private String profilePictureUrl; // S3 Linki

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner; // Sahibi
}