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
    private SharedUtilityService sharedUtilityService;
    @Autowired
    private LogbookService logbookService;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Logbook.class);
        mongoTemplate.remove(new Query(), Authorization.class);
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
                                .authorizations(
                                        List.of(
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType("Read")
                                                        .owner("user2@slac.stanford.edu")
                                                        .build()
                                        )
                                )
                                .build()
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

    @Test
    public void updateAuthorizationWithDeleteOk() {
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
                                .authorizations(
                                        List.of(
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType("Read")
                                                        .owner("user2@slac.stanford.edu")
                                                        .build(),
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType("Read")
                                                        .owner("userTODelete@slac.stanford.edu")
                                                        .build(),
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType("Read")
                                                        .owner("user3@slac.stanford.edu")
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        var logbook = assertDoesNotThrow(
                () -> logbookService.getLogbook(
                        newLogbookId,
                        Optional.of(true)
                )
        );
        assertThat(logbook).isNotNull();
        assertThat(logbook.authorizations()).isNotNull().hasSize(3);

        // updating deleting one authorizations
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
                                .authorizations(
                                        List.of(
                                                logbook.authorizations().get(0),
                                                logbook.authorizations().get(2)
                                        )
                                )
                                .build()
                )
        );

        var logbook2 = assertDoesNotThrow(
                () -> logbookService.getLogbook(
                        newLogbookId,
                        Optional.of(true)
                )
        );
        assertThat(logbook2).isNotNull();
        assertThat(logbook2.authorizations()).isNotNull()
                .hasSize(2)
                .extracting(AuthorizationDTO::owner)
                .contains("user2@slac.stanford.edu","user3@slac.stanford.edu");
    }

    @Test
    public void updateAuthorizationChangingAuthTypeOk() {
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
                                .authorizations(
                                        List.of(
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType(Authorization.Type.Read.name())
                                                        .owner("user2@slac.stanford.edu")
                                                        .build(),
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType(Authorization.Type.Read.name())
                                                        .owner("userTOUpdate@slac.stanford.edu")
                                                        .build(),
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType(Authorization.Type.Read.name())
                                                        .owner("user3@slac.stanford.edu")
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        var logbook = assertDoesNotThrow(
                () -> logbookService.getLogbook(
                        newLogbookId,
                        Optional.of(true)
                )
        );
        assertThat(logbook).isNotNull();
        assertThat(logbook.authorizations()).isNotNull()
                .hasSize(3)
                .extracting(AuthorizationDTO::authorizationType)
                .contains(
                        Authorization.Type.Read.name(),
                        Authorization.Type.Read.name(),
                        Authorization.Type.Read.name()
                );

        // updating deleting one authorizations
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
                                .authorizations(
                                        List.of(
                                                logbook.authorizations().get(0),
                                                logbook.authorizations().get(1)
                                                        .toBuilder()
                                                        .authorizationType(Authorization.Type.Write.name())
                                                        .build(),
                                                logbook.authorizations().get(2)
                                        )
                                )
                                .build()
                )
        );

        var logbook2 = assertDoesNotThrow(
                () -> logbookService.getLogbook(
                        newLogbookId,
                        Optional.of(true)
                )
        );
        assertThat(logbook2).isNotNull();
        assertThat(logbook2.authorizations()).isNotNull()
                .hasSize(3)
                .extracting(AuthorizationDTO::authorizationType)
                .contains(
                        Authorization.Type.Read.name(),
                        Authorization.Type.Write.name(),
                        Authorization.Type.Read.name()
                );
    }
}
