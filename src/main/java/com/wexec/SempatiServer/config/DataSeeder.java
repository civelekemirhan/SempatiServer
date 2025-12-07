package com.wexec.SempatiServer.config;

import com.wexec.SempatiServer.entity.*; // Gender ve ProfileIcon enumları için
import com.wexec.SempatiServer.repository.PostRepository;
import com.wexec.SempatiServer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // 1. ADIM: Admin Kullanıcıyı Oluştur
        String adminEmail = "admin@sempati.com";
        User testUser;

        Optional<User> existingUser = userRepository.findByEmail(adminEmail);

        if (existingUser.isPresent()) {
            testUser = existingUser.get();
        } else {
            testUser = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode("123123"))
                    .nickname("SempatiAdmin")
                    .role(Role.USER) // veya Role.ADMIN
                    .enabled(true)

                    // --- EKLENEN KISIMLAR ---
                    .gender(Gender.PREFER_NOT_SAY) // Zorunlu alan hatasını çözer
                    .profileIcon(ProfileIcon.ICON1) // Null kalmaması iyi olur
                    // ------------------------

                    .bio("Sempati uygulamasının otomatik oluşturulan test kullanıcısı.")
                    .build();

            testUser = userRepository.save(testUser);
            System.out.println("--- Test kullanıcısı oluşturuldu: admin@sempati.com / 123123 ---");
        }

        // 2. ADIM: Postları Oluştur
        if (postRepository.count() > 0) {
            System.out.println("Veritabanında zaten post var, yeni post eklenmedi.");
            return;
        }

        List<Post> dummyPosts = new ArrayList<>();
        Random random = new Random();

        for (PostType type : PostType.values()) {
            for (int i = 1; i <= 10; i++) {

                double lat = 41.0 + (random.nextDouble() * 0.5);
                double lon = 28.0 + (random.nextDouble() * 0.5);

                Post post = Post.builder()
                        .description("Otomatik test verisi #" + i + " - Tür: " + type.name())
                        .type(type)
                        .user(testUser)
                        .address("Test Mahallesi, Otomatik Sokak No:" + i)
                        .latitude(lat)
                        .longitude(lon)
                        .createdAt(LocalDateTime.now().minusHours(random.nextInt(100)))
                        .mediaUrls(new ArrayList<>())
                        .taggedPets(new ArrayList<>())
                        .build();

                dummyPosts.add(post);
            }
        }

        postRepository.saveAll(dummyPosts);
        System.out.println("--- BAŞARILI: " + dummyPosts.size() + " adet test postu eklendi! ---");
    }
}