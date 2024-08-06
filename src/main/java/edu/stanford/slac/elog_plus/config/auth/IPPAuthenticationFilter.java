package edu.stanford.slac.elog_plus.config.auth;


import com.hp.jipp.encoding.IppInputStream;
import com.hp.jipp.encoding.NameType;
import com.hp.jipp.trans.IppPacketData;
import edu.stanford.slac.ad.eed.baselib.auth.jwt.SLACAuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;

import static com.hp.jipp.encoding.Tag.jobAttributes;


@Log4j2
public class IPPAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    private final AppProperties appProperties;

    public IPPAuthenticationFilter(final String matcher,
                                   AuthenticationManager authenticationManager,
                                   AppProperties appProperties) {
        super(matcher);
        super.setAuthenticationManager(authenticationManager);
        this.appProperties = appProperties;
    }


    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException {
        if (request.getContentType()==null || !"application/ipp".equals(request.getContentType())) {
            // If media type is not "application/ipp", pass the request to the next filter in the chain
            throw new AuthenticationServiceException("Unsupported media type");
        }
        Authentication auth = null;
        IppInputStream inputStream = new IppInputStream(request.getInputStream());
        IppPacketData data = new IppPacketData(inputStream.readPacket(), inputStream);
        String jwtIppAuthenticationToken = data.getPacket().getString(jobAttributes, new NameType.Set("jwt"));
        if (jwtIppAuthenticationToken == null) {
            auth = SLACAuthenticationToken.builder().build();
        } else {
            log.debug("Received IPP JWT token: " + data.getPacket().prettyPrint(100, " "));
            auth = SLACAuthenticationToken
                    .builder()
                    .userToken(jwtIppAuthenticationToken)
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
