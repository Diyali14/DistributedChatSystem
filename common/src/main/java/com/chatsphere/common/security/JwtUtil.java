package com.chatsphere.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;

public class JwtUtil {

    private static final String SECRET_STRING = "ChatSphereXSuperSecretSigningKeyMustBeAtLeast32BytesLong!";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    public static String generateAccessToken(String userId, String username, List<String> roles, String sessionId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("roles", roles)
                .claim("sessionId", sessionId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + 900000)) // 15 minutes
                .signWith(SECRET_KEY)
                .compact();
    }

    public static String generateRefreshToken(String userId, String sessionId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .claim("sessionId", sessionId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + 604800000)) // 7 days
                .signWith(SECRET_KEY)
                .compact();
    }

    public static Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static boolean validateToken(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public static String getUserId(String token) {
        return extractClaims(token).getSubject();
    }

    public static String getSessionId(String token) {
        return extractClaims(token).get("sessionId", String.class);
    }
}
