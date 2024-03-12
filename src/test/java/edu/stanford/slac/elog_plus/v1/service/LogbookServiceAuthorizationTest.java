package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthenticationTokenDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.exception.AuthenticationTokenNotFound;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.AuthorizationMalformed;
import edu.stanford.slac.elog_plus.exception.DoubleAuthorizationError;
import edu.stanford.slac.elog_plus.model.Logbook;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO.Token;
import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO.User;
import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Read;
import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Write;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
    }

    @Test
    public void updateAuthorizationFailsOnMalformed() {
        Authentication authentication = sharedUtilityService.getAuthenticationMockForFirstRootUser();
        var newLogbookId =
                assertDoesNotThrow(
                        () -> sharedUtilityService.getTestLogbook("logbook-test-auth")
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // fails no owner
        assertThrows(
                AuthorizationMalformed.class,
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
                                                        .authorizationType(Read)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        // fails no ownerType
        assertThrows(
                AuthorizationMalformed.class,
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
                                                        .owner("user1@slac.stanford.edu")
                                                        .authorizationType(Read)
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
    }


    @Test
    public void updateAuthorizationFailsOnMultipleUserAuth() {
        Authentication authentication = sharedUtilityService.getAuthenticationMockForFirstRootUser();
        var newLogbookId =
                assertDoesNotThrow(
                        () -> sharedUtilityService.getTestLogbook("logbook-test-auth")
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // fails no owner
        DoubleAuthorizationError doubleAuthErr = assertThrows(
                DoubleAuthorizationError.class,
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
                                                        .authorizationType(Read)
                                                        .ownerType(User)
                                                        .owner("user1@slac.stanford.edu")
                                                        .build(),
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType(Write)
                                                        .ownerType(User)
                                                        .owner("user1@slac.stanford.edu")
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        assertThat(doubleAuthErr.getErrorCode()).isEqualTo(-1);
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
                                                        .authorizationType(Write)
                                                        .owner("user2@slac.stanford.edu")
                                                        .ownerType(User)
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
                                                        .authorizationType(Read)
                                                        .owner("user2@slac.stanford.edu")
                                                        .ownerType(User)
                                                        .build(),
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType(Read)
                                                        .owner("userTODelete@slac.stanford.edu")
                                                        .ownerType(User)
                                                        .build(),
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType(Read)
                                                        .owner("user3@slac.stanford.edu")
                                                        .ownerType(User)
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

        // updating deleting one authorization
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
                .contains("user2@slac.stanford.edu", "user3@slac.stanford.edu");
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
                                                        .authorizationType(Read)
                                                        .owner("user2@slac.stanford.edu")
                                                        .ownerType(User)
                                                        .build(),
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType(Read)
                                                        .owner("userTOUpdate@slac.stanford.edu")
                                                        .ownerType(User)
                                                        .build(),
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType(Read)
                                                        .owner("user3@slac.stanford.edu")
                                                        .ownerType(User)
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
                        Read,
                        Read,
                        Read
                );

        // updating deleting one authorization
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
                                                        .authorizationType(Write)
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
                        Read,
                        Write,
                        Read
                );
    }

    @Test
    public void updateAuthorizationOnApplicationToken() {
        Authentication authentication = sharedUtilityService.getAuthenticationMockForFirstRootUser();
        var newLogbookId =
                assertDoesNotThrow(
                        () -> sharedUtilityService.getTestLogbook("logbook-test-auth")
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // fails no owner
        var updatedLogbook = assertDoesNotThrow(
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
                                                        .authorizationType(Read)
                                                        .ownerType(Token)
                                                        .owner(sharedUtilityService.getTokenEmailForLogbookToken("tok-a", "logbook-test-auth"))
                                                        .build()
                                        )
                                )
                                .authenticationTokens(
                                        List.of(
                                                AuthenticationTokenDTO
                                                        .builder()
                                                        .name("tok-a")
                                                        .expiration(LocalDate.of(2023, 12, 31))
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        // the authorization need to contain an owner named tok-a@logbook-test-auth.elog.app
        assertThat(updatedLogbook.authorizations())
                .extracting("owner").contains("tok-a@logbook-test-auth.elog.slac.app$");
    }

    @Test
    public void updateAuthorizationFailOnBadApplicationToken() {
        Authentication authentication = sharedUtilityService.getAuthenticationMockForFirstRootUser();
        var newLogbookId =
                assertDoesNotThrow(
                        () -> sharedUtilityService.getTestLogbook("logbook-test-auth")
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // fails no owner
        AuthenticationTokenNotFound tokeNotFoundForAuthentication = assertThrows(
                AuthenticationTokenNotFound.class,
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
                                                        .authorizationType(Read)
                                                        .ownerType(Token)
                                                        .owner("tok-a")
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        // the authorization need to contain an owner named tok-a@logbook-test-auth.elog.app
        assertThat(tokeNotFoundForAuthentication.getErrorCode()).isEqualTo(-7);
    }

    @Test
    public void updateAuthorizationOnApplicationTokenUpdatingOneAlreadyProcessed() {
        Authentication authentication = sharedUtilityService.getAuthenticationMockForFirstRootUser();
        var newLogbookId =
                assertDoesNotThrow(
                        () -> sharedUtilityService.getTestLogbook("logbook-test-auth")
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // fails no owner
        var updatedLogbook = assertDoesNotThrow(
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
                                                        .authorizationType(Read)
                                                        .ownerType(Token)
                                                        .owner(sharedUtilityService.getTokenEmailForLogbookToken("tok-a", "logbook-test-auth"))
                                                        .build()
                                        )
                                )
                                .authenticationTokens(
                                        List.of(
                                                AuthenticationTokenDTO
                                                        .builder()
                                                        .name("tok-a")
                                                        .expiration(LocalDate.of(2023, 12, 31))
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        // the authorization need to contain an owner named tok-a@logbook-test-auth.elog.app
        assertThat(updatedLogbook.authorizations())
                .extracting("owner").contains("tok-a@logbook-test-auth.elog.slac.app$");
        var processedAuthorization = updatedLogbook.authorizations().get(0);
        var processedAuthentication = updatedLogbook.authenticationTokens().get(0);
        updatedLogbook = assertDoesNotThrow(
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
                                                processedAuthorization,
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType(Read)
                                                        .ownerType(Token)
                                                        .owner(sharedUtilityService.getTokenEmailForLogbookToken("tok-b", "logbook-test-auth")) //tok-b@<logbook name>.elog.slac.app$
                                                        .build()
                                        )
                                )
                                .authenticationTokens(
                                        List.of(
                                                processedAuthentication,
                                                AuthenticationTokenDTO
                                                        .builder()
                                                        .name("tok-b")
                                                        .expiration(LocalDate.of(2023, 12, 31))
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(updatedLogbook.authorizations())
                .extracting("owner")
                .contains(
                        "tok-a@logbook-test-auth.elog.slac.app$",
                        "tok-b@logbook-test-auth.elog.slac.app$"
                );
    }

    @Test
    public void deleteAuthenticationTokenWillDeleteTheAuthorization() {
        Authentication authentication = sharedUtilityService.getAuthenticationMockForFirstRootUser();
        var newLogbookId =
                assertDoesNotThrow(
                        () -> sharedUtilityService.getTestLogbook("logbook-test-auth")
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // fails no owner
        var updatedLogbook = assertDoesNotThrow(
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
                                                        .authorizationType(Read)
                                                        .ownerType(Token)
                                                        .owner(sharedUtilityService.getTokenEmailForLogbookToken("tok-a", "logbook-test-auth"))
                                                        .build(),
                                                AuthorizationDTO
                                                        .builder()
                                                        .authorizationType(Read)
                                                        .ownerType(Token)
                                                        .owner(sharedUtilityService.getTokenEmailForLogbookToken("tok-b", "logbook-test-auth"))
                                                        .build()
                                        )
                                )
                                .authenticationTokens(
                                        List.of(
                                                AuthenticationTokenDTO
                                                        .builder()
                                                        .name("tok-a")
                                                        .expiration(LocalDate.of(2023, 12, 31))
                                                        .build(),
                                                AuthenticationTokenDTO
                                                        .builder()
                                                        .name("tok-b")
                                                        .expiration(LocalDate.of(2023, 12, 31))
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        // the authorization need to contain an owner named tok-a@logbook-test-auth.elog.app
        assertThat(updatedLogbook.authorizations())
                .extracting("owner").contains("tok-a@logbook-test-auth.elog.slac.app$", "tok-b@logbook-test-auth.elog.slac.app$");
        //we are going to reproduce the situation where the uer delete a token but not the authorization
        //in this case an error is fired. Each time the token is removed also the authorization needs to be erased.
        var processedAuthorizations = updatedLogbook.authorizations();
        var processedAuthTokens = updatedLogbook.authenticationTokens();
        AuthenticationTokenNotFound authNotFoundExcept = assertThrows(
                AuthenticationTokenNotFound.class,
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
                                        processedAuthorizations
                                )
                                .authenticationTokens(
                                        List.of(
                                                processedAuthTokens.get(0)
                                        )
                                )
                                .build()
                )
        );
        assertThat(authNotFoundExcept.getErrorCode())
                .isEqualTo(-7);
    }
}
