package edu.stanford.slac.elog_plus.v1.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.Key;

@Log4j2
@Component
@Profile("test")
public class SLACTidTestSignKeyResolver extends SigningKeyResolverAdapter {
    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        return JWTHelper.SECRET_KEY;
    }
}