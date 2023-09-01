package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.auth.SLACUserInfo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    public UserDetails getUserInfo(String userIdentifier) {
        return SLACUserInfo.builder()
                .email("test@com")
                .displayName("test User")
                .build();
    }

    public Authentication getUserAuthentication(String authenticationToken) {
        return null;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return null;
    }
}
