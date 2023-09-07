package edu.stanford.slac.elog_plus.auth.test_mock_auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.Key;

import static edu.stanford.slac.elog_plus.auth.test_mock_auth.JWTHelper.SECRET_KEY;

@Log4j2
@Component
@Profile("test")
public class SLACTidTestSignKeyResolver extends SigningKeyResolverAdapter {
    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        return SECRET_KEY;
    }
}