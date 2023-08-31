package edu.stanford.slac.elog_plus.auth.k8s_slac;

import io.jsonwebtoken.Claims;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@Scope("prototype")
@NoArgsConstructor
public class SLACAuthenticationProvider implements AuthenticationProvider {
    @Override
    public boolean supports(Class<?> authentication) {
        return SLACAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (authentication.getPrincipal() == null) {
            return new SLACAuthenticationToken();
        }
        try {
            SLACAuthenticationToken slacToken = (SLACAuthenticationToken) authentication;
            Claims jwtBody = slacToken.getJwt().getBody();
            StringBuilder sb = new StringBuilder();
            if(jwtBody.containsKey("email")) {
                sb.append("email: %s ".formatted(jwtBody.get("email").toString()));
            }
            if(jwtBody.containsKey("name")) {
                sb.append("name: %s ".formatted(jwtBody.get("name").toString()));
            }
            if(jwtBody.containsKey("email_verified")) {
                sb.append("email_verified: %s ".formatted(jwtBody.get("email_verified").toString()));
            }
            log.debug("Logged user -> {}", sb.toString());
            //TODO load the user information and all the grant
            return new SLACAuthenticationToken(slacToken.getUserToken(), slacToken.getJwt());
        } catch (Throwable e) {
            log.error("{}", e.toString());
            throw new BadCredentialsException("Invalid token signature");
        }

    }
}
