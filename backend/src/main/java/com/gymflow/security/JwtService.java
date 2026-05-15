package com.gymflow.security;

import com.gymflow.api.error.BadRequestException;
import com.gymflow.domain.User;
import com.gymflow.domain.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.getSecret()));
    }

    public String issueToken(User user) {
        Long organizationId = user.getOrganization() == null ? null : user.getOrganization().getId();
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.getAccessTokenExpirationMinutes() * 60);

        return Jwts.builder()
            .subject(user.getEmail())
            .claim("userId", user.getId())
            .claim("organizationId", organizationId)
            .claim("role", user.getRole().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(secretKey)
            .compact();
    }

    public AuthenticatedUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            return new AuthenticatedUser(
                extractLongClaim(claims, "userId"),
                claims.getSubject(),
                Role.valueOf(claims.get("role", String.class)),
                extractLongClaim(claims, "organizationId")
            );
        } catch (Exception ex) {
            throw new BadRequestException("Invalid or expired access token");
        }
    }

    private Long extractLongClaim(Claims claims, String claimName) {
        Object value = claims.get(claimName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }
}
