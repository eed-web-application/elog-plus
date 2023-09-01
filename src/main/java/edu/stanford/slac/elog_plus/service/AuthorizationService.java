package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.auth.SLACUserInfo;
import edu.stanford.slac.elog_plus.auth.k8s_slac.SLACAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    public UserDetails getUserInfo(String userIdentifier) {
        return SLACUserInfo.builder()
                .email("test@com")
                .displayName("test User")
                .build();
    }

    public Authentication getUserAuthentication(String authenticationToken) {
        return new SLACAuthenticationToken(
                authenticationToken
        );
    }
}
