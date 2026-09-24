package com.nguyendat.shopee_be.config;

import com.nguyendat.shopee_be.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JWTTokenHelper {

    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.auth.app}")
    private String appName;

    @Value("${jwt.auth.secret_key}")
    private String secretKey;

    @Value("${jwt.auth.expires_in}")
    private long expiresIn;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(appName)
                .subject(user.getEmail())
                .claim("role", user.getPrimaryRole())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiresIn * 1000L))
                .signWith(signingKey)
                .compact();
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    // Lấy token từ header "Authorization: Bearer <token>"
    public String getToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }

    // Trả về email (subject) nếu token hợp lệ và chưa hết hạn, ngược lại null
    public String getUserNameFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims == null ? null : claims.getSubject();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        String username = getUserNameFromToken(token);
        return username != null && username.equals(userDetails.getUsername());
    }

    private Claims parseClaims(String token) {
        if (token == null) {
            return null;
        }
        try {
            // parseSignedClaims đã kiểm tra chữ ký và thời hạn token
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(appName)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
