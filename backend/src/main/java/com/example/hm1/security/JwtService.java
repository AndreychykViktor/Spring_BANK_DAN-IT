package com.example.hm1.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.SignatureAlgorithm;

import com.example.hm1.security.SecurityProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final String secretBase64;
    private final long expirationMillis;

    public JwtService(SecurityProperties props) {
        this.secretBase64 = props.getSecret();
        this.expirationMillis = props.getExpirationMillis();
    }

    public String extractUsername(String token) {
        return extractClaim(token, claims -> (String) claims.get("sub"));
    }

    public <T> T extractClaim(String token, Function<Map<String, Object>, T> resolver) {
        Map<String, Object> claims = parseAllClaims(token);
        return resolver.apply(claims);
    }

    public String generateToken(String username, String[] roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .claim("roles", roles)
                .subject(username)
                .issuedAt(now)
                .expiration(exp)
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, String username) {
        String sub = extractUsername(token);
        return sub != null && sub.equals(username) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        Date exp = extractClaim(token, claims -> (Date) claims.get("exp"));
        return exp.before(new Date());
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretBase64);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Map<String, Object> parseAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}