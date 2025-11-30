package com.wexec.SempatiServer.security;

import com.wexec.SempatiServer.entity.User; // User entity importu önemli
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // --- GÜNCELLENDİ: Token üretirken versiyonu da ekle ---
    public String generateAccessToken(User user) { // Parametreyi UserDetails yerine User yaptım
        Map<String, Object> claims = new HashMap<>();
        claims.put("v", user.getTokenVersion()); // "v" anahtarıyla versiyonu gömüyoruz
        return buildToken(claims, user, 1000 * 60 * 60); // 1 Saat
    }

    // Bu metod UserDetails aldığı için cast etmemiz gerekebilir, o yüzden User alan
    // versiyonu kullanmak daha iyi
    public String generateAccessToken(UserDetails userDetails) {
        if (userDetails instanceof User) {
            return generateAccessToken((User) userDetails);
        }
        return buildToken(new HashMap<>(), userDetails, 1000 * 60 * 60);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, 1000 * 60 * 60 * 24 * 30); // 30 Gün
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // --- GÜNCELLENDİ: Versiyon Kontrolü ---
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);

        // 1. Kullanıcı adı kontrolü
        if (!username.equals(userDetails.getUsername())) {
            return false;
        }

        // 2. Süre kontrolü
        if (isTokenExpired(token)) {
            return false;
        }

        // 3. Versiyon Kontrolü (Critical Part)
        // Eğer kullanıcı User sınıfındansa versiyonu kontrol et
        if (userDetails instanceof User) {
            User user = (User) userDetails;
            Long tokenVersion = extractClaim(token, claims -> {
                // JWT numeric değerleri bazen Integer bazen Long dönebilir, güvenli dönüşüm:
                Number v = claims.get("v", Number.class);
                return v != null ? v.longValue() : null;
            });

            // Eğer tokenda versiyon yoksa (eski tokensa) veya versiyonlar eşleşmiyorsa
            // GEÇERSİZDİR.
            // Not: İlk kayıtta tokenVersion null olabilir, onu 0 kabul ediyoruz.
            long currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
            long claimVersion = tokenVersion != null ? tokenVersion : 0L;

            return currentVersion == claimVersion;
        }

        return true;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}