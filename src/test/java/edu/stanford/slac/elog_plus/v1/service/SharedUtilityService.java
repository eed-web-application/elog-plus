package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.NewAuthenticationTokenDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import edu.stanford.slac.elog_plus.api.v1.dto.NewLogbookDTO;
import edu.stanford.slac.elog_plus.service.LogbookService;
import org.assertj.core.api.AssertionsForClassTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;

import static edu.stanford.slac.elog_plus.utility.StringUtilities.logbookNameNormalization;
import static edu.stanford.slac.elog_plus.utility.StringUtilities.tokenNameNormalization;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Service
public class SharedUtilityService {
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private LogbookService logbookService;
    @Autowired
    private AuthService authService;
    @Autowired
    private PeopleGroupService peopleGroupService;

    /**
     * Create a new token and return the email of the token
     * @param tokenName the name of the token
     * @return the email of the token
     */
    public String createNewToken(String tokenName, LocalDate expiration) {
        var newAuthenticationToken = assertDoesNotThrow(
                () -> authService.addNewApplicationAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name(tokenName)
                                .expiration(expiration)
                                .build(),
                        false
                )
        );

        return newAuthenticationToken.email();
    }

    public String getTestLogbook() {
        return getTestLogbook("new-logbooks");
    }

    public String getTestLogbook(String logbookName) {
        String newLogbookID = assertDoesNotThrow(
                () -> logbookService.createNew(
                        NewLogbookDTO
                                .builder()
                                .name(logbookName)
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(newLogbookID).isNotNull().isNotEmpty();
        return newLogbookID;
    }

    public String getTokenEmailForApplicationToken(String tokenName) {
        return "%s@%s".formatted(
                tokenNameNormalization(tokenName),
                appProperties.getAppEmailPostfix());
    }

    public String getTokenEmailForGlobalToken(String tokenName) {
        return "%s@%s".formatted(
                tokenNameNormalization(tokenName),
                appProperties.getAuthenticationTokenDomain());
    }

    public Authentication getAuthenticationMockForFirstRootUser() {
        if(appProperties.getRootUserList().isEmpty()) {
            return null;
        } else {
            return getAuthenticationMockForEmail(
                    appProperties.getRootUserList().get(0)
            );
        }
    }


    public PersonDTO getPersonForEmail(String mail) {
        return peopleGroupService.findPerson(getAuthenticationMockForEmail(mail));
    }

    public Authentication getAuthenticationMockForEmail(String email) {
        return new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return null;
            }

            @Override
            public Object getCredentials() {
                return email;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return email;
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

            }

            @Override
            public String getName() {
                return email;
            }
        };
    }

}
