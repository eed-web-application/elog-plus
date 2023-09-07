package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.elog_plus.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.TagDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.UpdateLogbookDTO;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.AuthService;
import edu.stanford.slac.elog_plus.service.LogbookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})

public class LogbookServiceAuthorizationTest {
    @Autowired
    AppProperties appProperties;
    @Autowired
    private SharedUtilityService sharedUtilityService;
    @Autowired
    private LogbookService logbookService;
    @Autowired
    private AuthService authService;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Logbook.class);
        mongoTemplate.remove(new Query(), Authorization.class);
        authService.updateRootUser();
    }

    @Test
    public void updateAuthorizationOk() {
        Authentication authentication = sharedUtilityService.getAuthenticationMockForFirstRootUser();
        var newLogbookId =
                assertDoesNotThrow(
                        () -> sharedUtilityService.getTestLogbook("logbook-test-auth")
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        assertDoesNotThrow(
                () -> logbookService.update(
                        newLogbookId,
                        UpdateLogbookDTO
                                .builder()
                                .name(
                                        "logbook-test-auth"
                                )
                                .tags(
                                        emptyList()
                                )
                                .shifts(
                                        emptyList()
                                )
                                .authorization(
                                        List.of(
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType("Read")
                                                        .owner("user2@slac.stanford.edu")
                                                        .build()
                                        )
                                )
                                .build(),
                        authentication
                )
        );

        var logbook = assertDoesNotThrow(
                () -> logbookService.getLogbook(
                        newLogbookId,
                        Optional.of(true)
                )
        );
        assertThat(logbook).isNotNull();
        assertThat(logbook.authorizations()).isNotNull().hasSize(1);
    }
}
