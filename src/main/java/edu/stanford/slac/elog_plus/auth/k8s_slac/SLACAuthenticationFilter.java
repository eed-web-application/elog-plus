package edu.stanford.slac.elog_plus.auth.k8s_slac;

import edu.stanford.slac.elog_plus.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.io.IOException;


@Log4j2
public class SLACAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    private final AppProperties appProperties;

    public SLACAuthenticationFilter(final String matcher,
                                    AuthenticationManager authenticationManager,
                                    AppProperties appProperties) {
        super(matcher);
        super.setAuthenticationManager(authenticationManager);
        this.appProperties = appProperties;
    }


    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        Authentication auth = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // print all header received
        String authenticationToken = request.getHeader(appProperties.getUserHeaderName());
        if (authenticationToken == null) {
            auth = SLACAuthenticationToken.builder().build();
        } else {
            auth = SLACAuthenticationToken
                    .builder()
                    .userToken(authenticationToken)
                    .build();
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
