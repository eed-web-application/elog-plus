package edu.stanford.slac.elog_plus.auth.k8s_slac;

import edu.stanford.slac.elog_plus.config.AppProperties;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Component
@Scope("prototype")
@AllArgsConstructor
public class SLACAuthenticationProvider implements AuthenticationProvider {
    private final AppProperties appProperties;

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

            Jwt j = Jwts.parserBuilder().setSigningKeyResolver(
                    SLACTidSignKeyResolver
                            .builder()
                            .discoverUrl(appProperties.getOauthServerDiscover())
                            .restTemplate(new RestTemplate())
                            .build()
            ).build().parse(((SLACAuthenticationToken) authentication).getUserToken());

//            Claims jwtBody = slacToken.getJwt().getBody();
//            StringBuilder sb = new StringBuilder();
//            if(jwtBody.containsKey("email")) {
//                sb.append("email: %s ".formatted(jwtBody.get("email").toString()));
//            }
//            if(jwtBody.containsKey("name")) {
//                sb.append("name: %s ".formatted(jwtBody.get("name").toString()));
//            }
//            if(jwtBody.containsKey("email_verified")) {
//                sb.append("email_verified: %s ".formatted(jwtBody.get("email_verified").toString()));
//            }
//            log.debug("Logged user -> {}", sb.toString());
            //TODO load the user information and all the grant
            return slacToken;
        } catch (Throwable e) {
            log.error("{}", e.toString());
            throw new BadCredentialsException("Invalid token signature");
        }

    }
}
