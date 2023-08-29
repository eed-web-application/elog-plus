package edu.stanford.slac.elog_plus.auth.k8s_slac;

import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.service.AuthorizationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.io.IOException;


public class SLACAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    private final AppProperties appProperties;
    private final AuthorizationService authorizationService;

    public SLACAuthenticationFilter(final String matcher,
                                    AuthenticationManager authenticationManager,
                                    AppProperties appProperties,
                                    AuthorizationService authorizationService) {
        super(matcher);
        super.setAuthenticationManager(authenticationManager);
        this.appProperties = appProperties;
        this.authorizationService = authorizationService;
    }


    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        Authentication auth = null;
        String authenticatedUserId = request.getHeader(appProperties.getUserHeaderName());
        if(authenticatedUserId == null) {
            auth = new SLACAuthenticationToken();
        } else {
            auth =  authorizationService.getUserAuthentication(authenticatedUserId);
        }
        return getAuthenticationManager().authenticate(auth);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        SecurityContextHolder.getContext().setAuthentication(authResult);
        chain.doFilter(request, response);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    }

}
