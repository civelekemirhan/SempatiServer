package com.wexec.SempatiServer.repository;

import com.wexec.SempatiServer.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long> {
    // Bir kullanıcının petlerini bulmak için
    List<Pet> findByOwnerId(Long ownerId);
}