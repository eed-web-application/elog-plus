package edu.stanford.slac.elog_plus.auth.test_mock_auth;

import edu.stanford.slac.elog_plus.auth.JWTHelper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.Key;

@Log4j2
@Component
@Profile("test")
@AllArgsConstructor
public class SLACTidTestSignKeyResolver extends SigningKeyResolverAdapter {
    JWTHelper jwtHelper;
    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        return jwtHelper.getKey();
    }
}