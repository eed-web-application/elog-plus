package edu.stanford.slac.elog_plus.config.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
@Log4j2
@Builder
@RequiredArgsConstructor
public class InstallServletRequestWrapper extends OncePerRequestFilter {
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<String> applyOnUri;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean shouldApply = applyOnUri.stream().anyMatch(uri -> pathMatcher.match(uri, request.getRequestURI()));
        // Continue the filter chain with the wrapped request
        if(shouldApply){
            log.debug("Wrapping request for URI: {}", request.getRequestURI());
            filterChain.doFilter(new CustomHttpServletRequestWrapper(request), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
