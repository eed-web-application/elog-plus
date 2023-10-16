package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.exception.*;
import edu.stanford.slac.elog_plus.model.*;
import edu.stanford.slac.elog_plus.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@PropertySource("classpath:application.yml")
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private AuthService authService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestControllerHelperService testControllerHelperService;

    @BeforeEach
    public void preTest() {

        //reset authorizations
        mongoTemplate.remove(new Query(), Logbook.class);
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
        mongoTemplate.remove(new Query(), Authorization.class);
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
    }

    @Test
    public void getMe() {
        ApiResultResponse<PersonDTO> meResult = assertDoesNotThrow(
                () -> testControllerHelperService.getMe(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu")
                )
        );

        assertThat(meResult).isNotNull();
        assertThat(meResult.getErrorCode()).isEqualTo(0);
        assertThat(meResult.getPayload().uid()).isEqualTo("user1");
    }

    @Test
    public void getMeFailUnauthorized() {
        NotAuthorized userNotFoundException = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.getMe(
                        mockMvc,
                        status().isUnauthorized(),
                        Optional.empty()
                )
        );

        assertThat(userNotFoundException).isNotNull();
        assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void findUsersOK() {
        ApiResultResponse<List<PersonDTO>> meResult = assertDoesNotThrow(
                () -> testControllerHelperService.findUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of("name")
                )
        );

        assertThat(meResult).isNotNull();
        assertThat(meResult.getErrorCode()).isEqualTo(0);
        assertThat(meResult.getPayload()).hasSize(3);
    }

    @Test
    public void findUsersByNameOK() {
        ApiResultResponse<List<PersonDTO>> meResult = assertDoesNotThrow(
                () -> testControllerHelperService.findUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of("Name1")
                )
        );

        assertThat(meResult).isNotNull();
        assertThat(meResult.getErrorCode()).isEqualTo(0);
        assertThat(meResult.getPayload()).hasSize(1);
        assertThat(meResult.getPayload().get(0).gecos()).isEqualTo("Name1 Surname1");
    }

    @Test
    public void findUsersBySurnameOK() {
        ApiResultResponse<List<PersonDTO>> meResult = assertDoesNotThrow(
                () -> testControllerHelperService.findUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of("Surname1")
                )
        );

        assertThat(meResult).isNotNull();
        assertThat(meResult.getErrorCode()).isEqualTo(0);
        assertThat(meResult.getPayload()).hasSize(1);
        assertThat(meResult.getPayload().get(0).gecos()).isEqualTo("Name1 Surname1");
    }

    @Test
    public void findUsersFailUnauthorized() {
        NotAuthorized userNotFoundException = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.findUsers(
                        mockMvc,
                        status().isUnauthorized(),
                        Optional.empty(),
                        Optional.of("name")
                )
        );

        assertThat(userNotFoundException).isNotNull();
        assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void findGroupsOK() {
        ApiResultResponse<List<GroupDTO>> meResult = assertDoesNotThrow(
                () -> testControllerHelperService.findGroups(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of("group")
                )
        );

        assertThat(meResult).isNotNull();
        assertThat(meResult.getErrorCode()).isEqualTo(0);
        assertThat(meResult.getPayload()).hasSize(2);
    }

    @Test
    public void findGroupsFailUnauthorized() {
        NotAuthorized userNotFoundException = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.findGroups(
                        mockMvc,
                        status().isUnauthorized(),
                        Optional.empty(),
                        Optional.of("group")
                )
        );

        assertThat(userNotFoundException).isNotNull();
        assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createAuthenticationTokenFailsOnMalformed() {
        AuthenticationTokenMalformed userNotFoundException = assertThrows(
                AuthenticationTokenMalformed.class,
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isBadRequest(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewAuthenticationTokenDTO
                                .builder()
                                .build()
                )
        );

        assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);

        userNotFoundException = assertThrows(
                AuthenticationTokenMalformed.class,
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isBadRequest(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("tok-name")
                                .build()
                )
        );

        assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);

        userNotFoundException = assertThrows(
                AuthenticationTokenMalformed.class,
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isBadRequest(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewAuthenticationTokenDTO
                                .builder()
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );

        assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createAuthenticationTokenOK() {
        ApiResultResponse<AuthenticationTokenDTO> authToken = assertDoesNotThrow(
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        assertThat(authToken.getErrorCode()).isEqualTo(0);
    }

    @Test
    public void createAuthenticationTokenFailOnSameAppManagedToken() {
        appProperties.getRootAuthenticationTokenList().clear();
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-a")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );

        assertDoesNotThrow(
                () -> authService.updateAutoManagedRootToken()
        );

        ControllerLogicException exceptionForSaveAlreadyExistingToken = assertThrows(
                ControllerLogicException.class,
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().is5xxServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-root-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        assertThat(exceptionForSaveAlreadyExistingToken.getErrorCode()).isEqualTo(-3);
    }

    @Test
    public void createRootAuthorizationFailOnSameAppManagedToken() {
        appProperties.getRootAuthenticationTokenList().clear();
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-a")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );

        assertDoesNotThrow(
                () -> authService.updateAutoManagedRootToken()
        );

        ControllerLogicException notAuthorizedOnAppManagedToken = assertThrows(
                ControllerLogicException.class,
                ()->testControllerHelperService.createNewRootUser(
                        mockMvc,
                        status().isInternalServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        testControllerHelperService.getTokenEmailForGlobalToken("token-root-a")
                )
        );
        assertThat(notAuthorizedOnAppManagedToken.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void deleteRootAuthorizationFailOnSameAppManagedToken() {
        appProperties.getRootAuthenticationTokenList().clear();
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("token-root-a")
                        .expiration(LocalDate.of(3000, 12, 31))
                        .build()
        );

        assertDoesNotThrow(
                () -> authService.updateAutoManagedRootToken()
        );

        ControllerLogicException notAuthorizedOnAppManagedToken = assertThrows(
                ControllerLogicException.class,
                ()->testControllerHelperService.deleteRootUser(
                        mockMvc,
                        status().isInternalServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        testControllerHelperService.getTokenEmailForGlobalToken("token-root-a")
                )
        );
        assertThat(notAuthorizedOnAppManagedToken.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void getAllAuthenticationTokenOK() {
        ApiResultResponse<AuthenticationTokenDTO> authToken1 = assertDoesNotThrow(
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        assertThat(authToken1.getErrorCode()).isEqualTo(0);

        ApiResultResponse<AuthenticationTokenDTO> authToken2 = assertDoesNotThrow(
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-b")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        assertThat(authToken2.getErrorCode()).isEqualTo(0);

        ApiResultResponse<List<AuthenticationTokenDTO>> allToken =  assertDoesNotThrow(
                () -> testControllerHelperService.getAllAuthenticationToken(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu")
                )
        );
        assertThat(allToken.getErrorCode()).isEqualTo(0);
        assertThat(allToken.getPayload())
                .hasSize(2)
                .extracting(
                        AuthenticationTokenDTO::name
                )
                .contains(
                        "token-a",
                        "token-b"
                );
    }

    @Test
    public void deleteAllAuthenticationTokenOK() {
        ApiResultResponse<AuthenticationTokenDTO> authToken1 = assertDoesNotThrow(
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        assertThat(authToken1.getErrorCode()).isEqualTo(0);

        ApiResultResponse<AuthenticationTokenDTO> authToken2 = assertDoesNotThrow(
                () -> testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-b")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        assertThat(authToken2.getErrorCode()).isEqualTo(0);

        ApiResultResponse<List<AuthenticationTokenDTO>> allToken =  assertDoesNotThrow(
                () -> testControllerHelperService.getAllAuthenticationToken(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu")
                )
        );
        assertThat(allToken.getErrorCode()).isEqualTo(0);
        assertThat(allToken.getPayload())
                .hasSize(2)
                .extracting(
                        AuthenticationTokenDTO::name
                )
                .contains(
                        "token-a",
                        "token-b"
                );

        ApiResultResponse<Boolean> deleteOpResult = assertDoesNotThrow(
                () -> testControllerHelperService.deleteAuthenticationToken(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        authToken2.getPayload().id()
                )
        );
        assertThat(deleteOpResult.getErrorCode()).isEqualTo(0);

        // now we will have only one token
        allToken =  assertDoesNotThrow(
                () -> testControllerHelperService.getAllAuthenticationToken(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu")
                )
        );
        assertThat(allToken.getErrorCode()).isEqualTo(0);
        assertThat(allToken.getPayload())
                .hasSize(1)
                .extracting(
                        AuthenticationTokenDTO::name
                )
                .contains(
                        "token-a"
                );
    }

    @Test
    public void createAuthTokenAndMakItRootFailOnNotExistingPerson() {
        ControllerLogicException personNotFoundException = assertThrows(
                ControllerLogicException.class,
                ()->testControllerHelperService.createNewRootUser(
                        mockMvc,
                        status().isNotFound(),
                        Optional.of("user1@slac.stanford.edu"),
                        "wrong@user.email"
                )
        );

        assertThat(personNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createAuthTokenAndMakeItRootFailOnLogbookToken() {
        ControllerLogicException logbookTokenCannotBeRootExcept = assertThrows(
                ControllerLogicException.class,
                ()->testControllerHelperService.createNewRootUser(
                        mockMvc,
                        status().isInternalServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        testControllerHelperService.getTokenEmailForLogbookToken("token-a", "logbook")
                )
        );

        assertThat(logbookTokenCannotBeRootExcept.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createAuthTokenAndMakItRootFailOnNotExistingGlobalToken() {
        AuthenticationTokenNotFound tokenNotFoundException = assertThrows(
                AuthenticationTokenNotFound.class,
                ()->testControllerHelperService.createNewRootUser(
                        mockMvc,
                        status().isNotFound(),
                        Optional.of("user1@slac.stanford.edu"),
                        testControllerHelperService.getTokenEmailForGlobalToken("token-a")
                )
        );

        assertThat(tokenNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createRootForGlobalToken() {
        var authenticationCreationResponse = assertDoesNotThrow(
                ()->testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        assertThat(authenticationCreationResponse.getErrorCode()).isEqualTo(0);
        // create root user for token
        var rootUseCreatedResponse = assertDoesNotThrow(
                ()->testControllerHelperService.createNewRootUser(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        authenticationCreationResponse.getPayload().email()
                )
        );

        assertThat(rootUseCreatedResponse.getErrorCode()).isEqualTo(0);
        // check if token has been created
        var allRootUseResponse = assertDoesNotThrow(
                ()->testControllerHelperService.findAllRootUser(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu")
                )
        );
        assertThat(allRootUseResponse.getErrorCode()).isEqualTo(0);
        assertThat(allRootUseResponse.getPayload())
                .extracting(AuthorizationDTO::owner)
                .hasSize(2)
                .contains(authenticationCreationResponse.getPayload().email());

        var deleteRootUserResponse = assertDoesNotThrow(
                ()->testControllerHelperService.deleteRootUser(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        authenticationCreationResponse.getPayload().email()
                )
        );
        assertThat(deleteRootUserResponse.getErrorCode()).isEqualTo(0);

        // check if token has been delete (only the default one should be present)
        allRootUseResponse = assertDoesNotThrow(
                ()->testControllerHelperService.findAllRootUser(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu")
                )
        );
        assertThat(allRootUseResponse.getErrorCode()).isEqualTo(0);
        assertThat(allRootUseResponse.getPayload())
                .extracting(AuthorizationDTO::owner)
                .hasSize(1)
                .doesNotContain(authenticationCreationResponse.getPayload().email());
    }

    @Test
    public void createRootForLogbookToken() {
        var logbookCreationResponse = assertDoesNotThrow(
                ()->testControllerHelperService.getNewLogbookWithNameWithAuthorizationAndAppToken(
                        mockMvc,
                        Optional.of("user1@slac.stanford.edu"),
                        "new logbook",
                        List.of(
                                AuthorizationDTO
                                        .builder()
                                        .owner(testControllerHelperService.getTokenEmailForLogbookToken("token-a","new logbook"))
                                        .authorizationType(AuthorizationTypeDTO.Read)
                                        .ownerType("Application")
                                        .build()
                        ),
                        List.of(
                                AuthenticationTokenDTO
                                        .builder()
                                        .name("token-a")
                                        .expiration(LocalDate.of(2023,12,31))
                                        .build()
                        )
                )
        );
        assertThat(logbookCreationResponse.getErrorCode()).isEqualTo(0);
        // create root user for token
        var logbookTokenException = assertThrows(
                ControllerLogicException.class,
                ()->testControllerHelperService.createNewRootUser(
                        mockMvc,
                        status().isInternalServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        testControllerHelperService.getTokenEmailForLogbookToken("token-a","new logbook")
                )
        );
        assertThat(logbookTokenException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void checkIfRootUserCanCreateOtherRootUser() {
        var authenticationTokenCreationResponse = assertDoesNotThrow(
                ()->testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        assertThat(authenticationTokenCreationResponse.getErrorCode()).isEqualTo(0);
        // create root user for token
        var rootUseCreatedResponse = assertDoesNotThrow(
                ()->testControllerHelperService.createNewRootUser(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        authenticationTokenCreationResponse.getPayload().email()
                )
        );

        assertThat(rootUseCreatedResponse.getErrorCode()).isEqualTo(0);

        // create new token and root auth with this new token
        var anotherAuthenticationTokenCreationResponse = assertDoesNotThrow(
                ()->testControllerHelperService.createNewAuthenticationToken(
                        mockMvc,
                        status().isCreated(),
                        Optional.of(authenticationTokenCreationResponse.getPayload().email()),
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-b-made-by-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        assertThat(anotherAuthenticationTokenCreationResponse.getErrorCode()).isEqualTo(0);

        var anotheRootAuthorizationCreatedResponse = assertDoesNotThrow(
                ()->testControllerHelperService.createNewRootUser(
                        mockMvc,
                        status().isCreated(),
                        Optional.of(authenticationTokenCreationResponse.getPayload().email()),
                        anotherAuthenticationTokenCreationResponse.getPayload().email()
                )
        );

        assertThat(anotheRootAuthorizationCreatedResponse.getErrorCode()).isEqualTo(0);
    }
}
