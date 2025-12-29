package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.BusinessException;
import com.wexec.SempatiServer.common.ErrorCode;
import com.wexec.SempatiServer.common.GenericResponse;
import com.wexec.SempatiServer.dto.ChangePasswordRequest;
import com.wexec.SempatiServer.dto.PetDto;
import com.wexec.SempatiServer.dto.PostDto;
import com.wexec.SempatiServer.dto.UserProfileResponse;
import com.wexec.SempatiServer.dto.UserUpdateRequest;
import com.wexec.SempatiServer.entity.Post;
import com.wexec.SempatiServer.entity.ProfileIcon;
import com.wexec.SempatiServer.entity.User;
import com.wexec.SempatiServer.entity.UserBlock;

import org.springframework.security.crypto.password.PasswordEncoder;
import com.wexec.SempatiServer.repository.PostRepository;
import com.wexec.SempatiServer.repository.UserBlockRepository;
import com.wexec.SempatiServer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserBlockRepository userBlockRepository;

    // --- YENİ EKLENENLER ---
    private final PostRepository postRepository; // Veriyi çekmek için
    private final PostService postService; // Post -> PostDto çevirisi için
    // -----------------------

    // 1. Kendi Profilimi Getir
    @Transactional(readOnly = true) // Lazy loading hatası almamak için Transactional ekledik
    public GenericResponse<UserProfileResponse> getMyProfile() {
        User currentUser = getCurrentAuthenticatedUser();

        // 1. Kullanıcı bilgilerini DTO'ya çevir
        UserProfileResponse response = UserProfileResponse.fromEntity(currentUser);

        // 2. Kullanıcının postlarını Entity (Post) olarak çek (En yeni en üstte)
        List<Post> userPosts = postRepository.findAllByUserIdOrderByCreatedAtDesc(currentUser.getId());

        // 3. Entity Listesini -> DTO Listesine çevir
        // PostService içindeki convertToPostDto metodunu kullanıyoruz
        List<PostDto> postDtos = userPosts.stream()
                .map(postService::convertToPostDto) // Method reference kullanımı
                .collect(Collectors.toList());

        // 4. DTO listesini cevaba ekle
        response.setPosts(postDtos);

        return GenericResponse.success(response);
    }

    // 2. Başkasının Profilini Getir (ID ile)
    @Transactional(readOnly = true)
    public GenericResponse<UserProfileResponse> getUserProfileById(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "User ID boş olamaz.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1. Kullanıcı bilgilerini DTO'ya çevir
        UserProfileResponse response = UserProfileResponse.fromEntity(user);

        // 2. Kullanıcının postlarını Entity olarak çek
        List<Post> userPosts = postRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        // 3. Entity -> DTO Çevrimi
        List<PostDto> postDtos = userPosts.stream()
                .map(postService::convertToPostDto)
                .collect(Collectors.toList());

        // 4. Listeyi ekle
        response.setPosts(postDtos);

        return GenericResponse.success(response);
    }

    // 3. Profil İkonu Güncelleme
    @Transactional
    public GenericResponse<UserProfileResponse> updateProfileIcon(ProfileIcon newIcon) {
        if (newIcon == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "İkon boş olamaz.");
        }

        User currentUser = getCurrentAuthenticatedUser();

        currentUser.setProfileIcon(newIcon);
        userRepository.save(currentUser);

        // İkon güncelleyince postları çekmeye gerek yok, sadece kullanıcı bilgisini
        // dönüyoruz
        return GenericResponse.success(UserProfileResponse.fromEntity(currentUser));
    }

    // --- Yardımcı Metod: O anki kullanıcıyı bul ---
    private User getCurrentAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId;

        if (principal instanceof User) {
            userId = ((User) principal).getId();
        } else {
            throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // 4. Benim Petlerimi Getir
    @Transactional(readOnly = true)
    public GenericResponse<List<PetDto>> getMyPets() {
        // O anki kimliği doğrulanmış kullanıcıyı bul.
        User currentUser = getCurrentAuthenticatedUser();

        // 2. Kullanıcının pet listesini çek ve PetDto'ya dönüştür.
        List<PetDto> petDtos = currentUser.getPets() != null ? currentUser.getPets().stream()
                .map(PetDto::fromEntity)
                .collect(Collectors.toList())
                : new ArrayList<>(); // Kullanıcının petleri yoksa boş liste döner.

        return GenericResponse.success(petDtos);
    }

    // Profil Düzenleme (PATCH)
    @Transactional
    public GenericResponse<UserProfileResponse> updateUserProfile(UserUpdateRequest request) {
        // 1. O anki giriş yapmış kullanıcıyı bul
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. Veritabanından güncel halini çek (Garanti olsun)
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 3. Sadece dolu gelen alanları güncelle (PATCH Mantığı)

        if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
            user.setNickname(request.getNickname());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        if (request.getProfileIcon() != null) {
            user.setProfileIcon(request.getProfileIcon());
        }

        // 4. Kaydet
        User savedUser = userRepository.save(user);

        // 5. Güncel profili dön
        return GenericResponse.success(UserProfileResponse.fromEntity(savedUser));
    }

    // Şifre Değiştirme Metodu
    @Transactional
    public GenericResponse<String> changePassword(ChangePasswordRequest request) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User dbUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), dbUser.getPassword())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Mevcut şifreniz hatalı.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Yeni şifre en az 6 karakter olmalıdır.");
        }
        dbUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(dbUser);

        return GenericResponse.success("Şifreniz başarıyla güncellendi.");
    }

    public GenericResponse<String> updateFcmToken(String token) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        currentUser.setFcmToken(token);
        userRepository.save(currentUser);

        return GenericResponse.success("Bildirim tokenı güncellendi.");
    }

    public GenericResponse<String> clearFcmToken() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        currentUser.setFcmToken(null);
        userRepository.save(currentUser);

        return GenericResponse.success("Bildirim tokenı temizlendi.");
    }

    // Hesap Silme Metodu
    @Transactional
    public GenericResponse<String> deleteCurrentUserAccount() {

        User currentUser = getCurrentAuthenticatedUser();

        userRepository.delete(currentUser);

        return GenericResponse.success("Hesabınız ve tüm verileriniz başarıyla silinmiştir.");
    }

    // Kullanıcı Engelle
    @Transactional
    public GenericResponse<String> blockUser(Long userIdToBlock) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (currentUser.getId().equals(userIdToBlock)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Kendinizi engelleyemezsiniz.");
        }

        // Zaten engelli mi?
        if (userBlockRepository.existsByBlockerIdAndBlockedId(currentUser.getId(), userIdToBlock)) {
            return GenericResponse.success("Bu kullanıcı zaten engelli.");
        }

        User userToBlock = userRepository.findById(userIdToBlock)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserBlock block = UserBlock.builder()
                .blocker(currentUser)
                .blocked(userToBlock)
                .createdAt(LocalDateTime.now())
                .build();

        userBlockRepository.save(block);

        return GenericResponse.success("Kullanıcı engellendi.");
    }

    // Engeli Kaldır
    @Transactional
    public GenericResponse<String> unblockUser(Long userIdToUnblock) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        UserBlock block = userBlockRepository.findByBlockerIdAndBlockedId(currentUser.getId(), userIdToUnblock)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Engel kaydı bulunamadı."));

        userBlockRepository.delete(block);

        return GenericResponse.success("Kullanıcı engeli kaldırıldı.");
    }

}