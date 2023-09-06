package edu.stanford.slac.elog_plus.auth.test;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JWTHelper {
    public static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_TIME_MS = 3600000;
    // For use with MockMvc
    public static String generateJwt(String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME_MS);
        Map<String,Object> claims = new HashMap<>();
        claims.put("email", email);
        // Build the JWT
        return Jwts.builder()
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SECRET_KEY)
                .compact();
    }

    @Log4j2
    @Component
    @Profile("test")
    public static class SLACTidTestSignKeyResolver extends SigningKeyResolverAdapter {
        @Override
        public Key resolveSigningKey(JwsHeader header, Claims claims) {
            return SECRET_KEY;
        }
    }
}