package edu.stanford.slac.elog_plus.auth.test_mock_auth;

import edu.stanford.slac.elog_plus.auth.BaseSignKeyResolver;
import edu.stanford.slac.elog_plus.auth.JWTHelper;
import edu.stanford.slac.elog_plus.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.Key;

@Log4j2
@Component
@Profile("test")
public class SLACTidTestSignKeyResolver extends BaseSignKeyResolver {
    private final JWTHelper jwtHelper;
    public SLACTidTestSignKeyResolver(JWTHelper jwtHelper) {
        super(jwtHelper);
        this.jwtHelper = jwtHelper;
    }
    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        Key result = super.resolveSigningKey(header, claims);
        if(result == null) {
            result = jwtHelper.getKey();
        }
        return result;
    }
}