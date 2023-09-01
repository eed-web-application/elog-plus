package edu.stanford.slac.elog_plus.auth.k8s_slac;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

@Getter
public class SLACAuthenticationToken extends AbstractAuthenticationToken {
    private String userToken = null;
    public SLACAuthenticationToken() {
        super(Collections.emptyList());
        super.setAuthenticated(false);
    }

    public SLACAuthenticationToken(String userToken) {
        super(Collections.emptyList());
        super.setAuthenticated(true);
        this.userToken = userToken;
    }

    public SLACAuthenticationToken(String userUniqueId, Object details, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        super.setDetails(details);
        super.setAuthenticated(true);
        this.userToken = userUniqueId;
    }

    @Override
    public Object getCredentials() {
        return userToken;
    }

    @Override
    public Object getPrincipal() {
        return userToken;
    }
}
