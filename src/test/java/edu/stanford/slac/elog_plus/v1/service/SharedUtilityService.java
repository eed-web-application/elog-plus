package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.elog_plus.api.v1.dto.NewLogbookDTO;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.service.LogbookService;
import lombok.AllArgsConstructor;
import org.assertj.core.api.AssertionsForClassTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Service
public class SharedUtilityService {
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private LogbookService logbookService;
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


    public Authentication getAuthenticationMockForFirstRootUser() {
        if(appProperties.getRootUserList().isEmpty()) {
            return null;
        } else {
            return getAuthenticationMockForEmail(
                    appProperties.getRootUserList().get(0)
            );
        }
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
