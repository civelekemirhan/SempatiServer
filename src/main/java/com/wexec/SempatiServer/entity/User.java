package com.wexec.SempatiServer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;
    private String nickname;
    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean enabled; // Sadece bu kalıyor (True/False)

    // Token ilişkileri (Opsiyonel, veritabanı seviyesinde cascade silme için iyi
    // olur)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<RefreshToken> refreshTokens;

    // UserDetails methodları...
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Builder.Default // Eğer builder kullanıyorsan varsayılan değer ataması için
    @Column(nullable = false)
    private Long tokenVersion = 0L; // Varsayılan 0
}