package edu.stanford.slac.elog_plus.config;

import edu.stanford.slac.ad.eed.baselib.auth.jwt.SLACAuthenticationFilter;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.elog_plus.config.auth.IPPAuthenticationFilter;
import edu.stanford.slac.elog_plus.config.filters.InstallServletRequestWrapper;
import lombok.AllArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
import static org.springframework.data.mongodb.core.BulkOperations.BulkMode.ORDERED;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
//@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class IPPSecurityConfig {
    private final AppProperties appProperties;
    private final ApplicationContext applicationContext;

    @Bean
    public SecurityFilterChain ippFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disabling CSRF protection
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Set session management to stateless
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/**").permitAll() // Permit all requests
                        .anyRequest().authenticated() // All other requests need to be authenticated
                )
                .addFilterBefore(
                        new SLACAuthenticationFilter(
                                "/**",
                                applicationContext.getBean(AuthenticationManager.class),
                                appProperties
                        ),
                        AnonymousAuthenticationFilter.class
                )
                .addFilterBefore(
                        new InstallServletRequestWrapper(),
                        AnonymousAuthenticationFilter.class // Add CustomAuthenticationFilter before IPPAuthenticationFilter
                )
                .addFilterBefore(
                        new IPPAuthenticationFilter(
                                "/v1/printers/**",
                                applicationContext.getBean(AuthenticationManager.class),
                                appProperties
                        ),
                        AnonymousAuthenticationFilter.class
                );


        return http.build();
    }

}




