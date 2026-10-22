package ma.fsbm.lias.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenTtlMinutes;

    public JwtService(
            @Value("${lias.security.jwt.secret}") String secret,
            @Value("${lias.security.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes) {

        this.signingKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secret)
        );

        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    public String generateAccessToken(
            String email,
            Long membreId,
            Collection<String> authorities) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(email)
                .claim("membreId", membreId)
                .claim("authorities", authorities)
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES)
                ))
                .signWith(signingKey)
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public Long extractMembreId(String token) {
        Number membreId =
                (Number) extractClaims(token).get("membreId");

        return membreId == null
                ? null
                : membreId.longValue();
    }

    public List<String> extractAuthorities(String token) {

        List<?> authorities =
                extractClaims(token)
                        .get("authorities", List.class);

        if (authorities == null) {
            return List.of();
        }

        return authorities.stream()
                .map(Object::toString)
                .toList();
    }

    public boolean isTokenValid(String token) {
        try {

            Claims claims = extractClaims(token);

            return claims.getSubject() != null
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());

        } catch (Exception exception) {
            return false;
        }
    }

    private Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}