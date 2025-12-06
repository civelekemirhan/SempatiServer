package com.wexec.SempatiServer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

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
    private String genus;   // Kedi, Köpek
    private String breed;   // Cins (Tekir, Golden)

    private Integer age;        // YENİ: Doğum tarihi yerine yaş
    private boolean isNeutered; // YENİ: Kısırlaştırılmış mı?

    private String profilePictureUrl; // S3 Linki

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore // Sonsuz döngü olmasın diye
    private User owner;

    // Bir pet birçok postta etiketlenebilir
    @ManyToMany(mappedBy = "taggedPets")
    @JsonIgnore
    private List<Post> taggedInPosts;
}