package edu.stanford.slac.elog_plus.auth.k8s_slac;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;


public class SLACAuthenticationToken extends AbstractAuthenticationToken {
    private String userUniqueId = null;
    public SLACAuthenticationToken() {
        super(Collections.emptyList());
        super.setAuthenticated(false);
    }

    public SLACAuthenticationToken(String userUniqueId) {
        super(Collections.emptyList());
        super.setAuthenticated(true);
        this.userUniqueId = userUniqueId;
    }

    public SLACAuthenticationToken(String userUniqueId, Object details, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        super.setDetails(details);
        super.setAuthenticated(true);
        this.userUniqueId = userUniqueId;
    }

    @Override
    public Object getCredentials() {
        return userUniqueId;
    }

    @Override
    public Object getPrincipal() {
        return userUniqueId;
    }
}
