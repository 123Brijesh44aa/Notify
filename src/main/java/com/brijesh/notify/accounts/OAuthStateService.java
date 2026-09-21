package com.brijesh.notify.accounts;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class OAuthStateService {

    @Value("${jwt.secret}")  // reusing the same secret as app-login JWTs is fine here
    private String secret;

    public String createState(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 600_000)) // 10 minutes
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
    }

    public Long parseUserId(String state) {
        Claims claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes())).build()
                .parseSignedClaims(state).getPayload();
        return Long.valueOf(claims.getSubject());
    }
}
