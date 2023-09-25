package edu.stanford.slac.elog_plus.auth;

import edu.stanford.slac.elog_plus.api.v1.dto.NewAuthenticationTokenDTO;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.model.AuthenticationToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class JWTHelper {
    private final AppProperties appProperties;
    public final String applicationIssuer = "elog-plus";
    private static Key secretKey = null;
    private static final long EXPIRATION_TIME_MS = 3600000;
    // For use with MockMvc
    public String generateJwt(String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME_MS);
        Map<String,Object> claims = new HashMap<>();
        claims.put("email", email);
        // Build the JWT
        return Jwts.builder()
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getKey())
                .compact();
    }

    public String generateAuthenticationToken(NewAuthenticationTokenDTO authenticationToken) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("email", authenticationToken.name());
        // Build the JWT
        return Jwts.builder()
                .addClaims(claims)
                .setIssuedAt(new Date())
                .setIssuer(applicationIssuer)
                .setExpiration(
                        Date.from(
                                authenticationToken.expiration().atStartOfDay().toInstant(ZoneOffset.UTC)
                        )
                )
                .signWith(getKey())
                .compact();
    }

    public Key getKey() {
        if(secretKey==null) {
            byte[] keyBytes = hexStringToByteArray(appProperties.getAppTokenJwtKey());
            secretKey = Keys.hmacShaKeyFor(keyBytes);
        }
        return secretKey;
    }

    private byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
}