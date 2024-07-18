package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.*;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.Condition;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.*;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LogbookControllerAuthTest {
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
        mongoTemplate.remove(new Query(), Logbook.class);
        mongoTemplate.remove(new Query(), Entry.class);
        //reset authorizations
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
    }

    @Test
    public void testGetAllLogbookForAuthType() {
        var newLogbookApiResultOne = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook",
                List.of(
                        NewAuthorizationDTO
                                .builder()
                                .ownerId("user2@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .authorizationType(
                                        Write
                                )
                                .build(),
                        NewAuthorizationDTO
                                .builder()
                                .ownerId("user3@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .authorizationType(
                                        Read
                                )
                                .build()
                )
        );
        var newLogbookApiResultTwo = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook 2",
                List.of(
                        NewAuthorizationDTO
                                .builder()
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .ownerId("user2@slac.stanford.edu")
                                .authorizationType(
                                        Write
                                )
                                .build()
                )
        );
        var newLogbookApiResultThree = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook 3",
                List.of()
        );

        var allLogbookResultUser3 = assertDoesNotThrow(
                () -> testControllerHelperService.getAllLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user3@slac.stanford.edu"),
                        Optional.of(false),
                        Optional.empty()
                )
        );

        assertThat(allLogbookResultUser3)
                .isNotNull()
                .extracting(
                        ApiResultResponse::getErrorCode
                )
                .isEqualTo(0);
        assertThat(allLogbookResultUser3.getPayload())
                .hasSize(1)
                .extracting(LogbookDTO::id)
                .contains(
                        newLogbookApiResultOne.getPayload()
                );

        var allLogbookResultUser2 = assertDoesNotThrow(
                () -> testControllerHelperService.getAllLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user2@slac.stanford.edu"),
                        Optional.of(false),
                        Optional.empty()
                )
        );

        assertThat(allLogbookResultUser2)
                .isNotNull()
                .extracting(
                        ApiResultResponse::getErrorCode
                )
                .isEqualTo(0);
        assertThat(allLogbookResultUser2.getPayload())
                .hasSize(2)
                .extracting(LogbookDTO::id)
                .contains(
                        newLogbookApiResultOne.getPayload(),
                        newLogbookApiResultTwo.getPayload()
                );

        var allLogbookResultUser2Readable = assertDoesNotThrow(
                () -> testControllerHelperService.getAllLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user2@slac.stanford.edu"),
                        Optional.of(false),
                        Optional.of(
                                Read.name()
                        )
                )
        );

        assertThat(allLogbookResultUser2Readable)
                .isNotNull()
                .extracting(
                        ApiResultResponse::getErrorCode
                )
                .isEqualTo(0);
        assertThat(allLogbookResultUser2Readable.getPayload())
                .hasSize(2)
                .extracting(LogbookDTO::id)
                .contains(
                        newLogbookApiResultOne.getPayload()
                );

        var allLogbookResultUser1 = assertDoesNotThrow(
                () -> testControllerHelperService.getAllLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of(false),
                        Optional.empty()
                )
        );

        assertThat(allLogbookResultUser1)
                .isNotNull()
                .extracting(
                        ApiResultResponse::getErrorCode
                )
                .isEqualTo(0);
        assertThat(allLogbookResultUser1.getPayload())
                .hasSize(3)
                .extracting(LogbookDTO::id)
                .contains(
                        newLogbookApiResultOne.getPayload(),
                        newLogbookApiResultTwo.getPayload(),
                        newLogbookApiResultThree.getPayload()
                );
    }

    @Test
    public void testGetAllLogbookForAuthTypeUsingApplicationToken() {
        var tokensEmail = assertDoesNotThrow(
                () -> testControllerHelperService.applicationControllerCreateNewApplication(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewApplicationDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023, 12, 31))
                                .build()

                )
        );
        var newLogbookApiResultOne = testControllerHelperService.getNewLogbookWithNameWithAuthorizationAndAppToken(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook",
                List.of(
                        LogbookOwnerAuthorizationDTO
                                .builder()
                                .owner("user2@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .authorizationType(
                                        Write
                                )
                                .build(),
                        LogbookOwnerAuthorizationDTO
                                .builder()
                                .owner(tokensEmail.getPayload())
                                .ownerType(AuthorizationOwnerTypeDTO.Token)
                                .authorizationType(
                                        Read
                                )
                                .build()
                )
        );
        var newLogbookApiResultTwo = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook 2",
                List.of(
                        NewAuthorizationDTO
                                .builder()
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .ownerId("user2@slac.stanford.edu")
                                .authorizationType(
                                        Write
                                )
                                .build()
                )
        );

        var newLogbookApiResultThree = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook 3",
                List.of()
        );

        var allLogbookResultUser3 = assertDoesNotThrow(
                () -> testControllerHelperService.getAllLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user3@slac.stanford.edu"),
                        Optional.of(false),
                        Optional.empty()
                )
        );

        assertThat(allLogbookResultUser3)
                .isNotNull()
                .extracting(
                        ApiResultResponse::getErrorCode
                )
                .isEqualTo(0);
        assertThat(allLogbookResultUser3.getPayload())
                .hasSize(0);
    }

    @Test
    public void getAsReadAuthorizedUser() {
        var newLogbookApiResultOneReader = assertDoesNotThrow(
                () -> testControllerHelperService.getNewLogbookWithNameWithAuthorizationAndAppToken(
                        mockMvc,
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        "new logbook",
                        List.of(
                                LogbookOwnerAuthorizationDTO
                                        .builder()
                                        .owner("user2@slac.stanford.edu")
                                        .ownerType(AuthorizationOwnerTypeDTO.User)
                                        .authorizationType(
                                                Read
                                        )
                                        .build()
                        )
                )
        );
        var newLogbookApiResultTwoWriter = assertDoesNotThrow(
                () -> testControllerHelperService.getNewLogbookWithNameWithAuthorizationAndAppToken(
                        mockMvc,
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        "new logbook 2",
                        List.of(
                                LogbookOwnerAuthorizationDTO
                                        .builder()
                                        .owner("user2@slac.stanford.edu")
                                        .ownerType(AuthorizationOwnerTypeDTO.User)
                                        .authorizationType(
                                                Write
                                        )
                                        .build()
                        )
                )
        );
        var newLogbookApiResultThreeReader = assertDoesNotThrow(
                () -> testControllerHelperService.getNewLogbookWithNameWithAuthorizationAndAppToken(
                        mockMvc,
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        "new logbook 3",
                        List.of(
                                LogbookOwnerAuthorizationDTO
                                        .builder()
                                        .owner("user2@slac.stanford.edu")
                                        .ownerType(AuthorizationOwnerTypeDTO.User)
                                        .authorizationType(
                                                Read
                                        )
                                        .build()
                        )
                )
        );

        var logbookThaUser2CanRead = assertDoesNotThrow(
                () -> testControllerHelperService.getAllLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user2@slac.stanford.edu"),
                        Optional.of(false),
                        Optional.of(Read.name())
                )
        );
        assertThat(logbookThaUser2CanRead.getPayload())
                .hasSize(3)
                .extracting(LogbookDTO::id)
                .contains(
                        newLogbookApiResultOneReader.getPayload(),
                        newLogbookApiResultTwoWriter.getPayload(),
                        newLogbookApiResultThreeReader.getPayload()
                );

        var logbookThaUser2CanWrite = assertDoesNotThrow(
                () -> testControllerHelperService.getAllLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user2@slac.stanford.edu"),
                        Optional.of(false),
                        Optional.of(Write.name())
                )
        );
        assertThat(logbookThaUser2CanWrite.getPayload())
                .hasSize(1)
                .extracting(LogbookDTO::id)
                .contains(
                        newLogbookApiResultTwoWriter.getPayload()
                );
    }

    @Test
    public void testNewAuthorizationLogbookApiToSetUserAuth() {
        var newLogbookResult = assertDoesNotThrow(
                () -> testControllerHelperService.getNewLogbookWithNameWithAuthorizationAndAppToken(
                        mockMvc,
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        "new logbook",
                        emptyList()
                )
        );

        assertThat(newLogbookResult.getPayload()).isNotEmpty();

        var logbook = assertDoesNotThrow(
                () -> testControllerHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        newLogbookResult.getPayload(),
                        Optional.of(true)
                )
        );

        assertThat(logbook.getPayload())
                .isNotNull();
        assertThat(logbook.getPayload().authorizations())
                .isNullOrEmpty();

        // apply authorization for group
        assertDoesNotThrow(
                () -> testControllerHelperService.applyLogbookUserAuthorization(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        List.of(
                                LogbookUserAuthorizationDTO
                                        .builder()
                                        .logbookId(newLogbookResult.getPayload())
                                        .userId("user1@slac.stanford.edu")
                                        .authorizationType(Write)
                                        .build()
                        )
                )
        );
        // apply authorization for group
//        assertDoesNotThrow(
//                () -> testControllerHelperService.applyLogbookGroupAuthorizations(
//                        mockMvc,
//                        status().isCreated(),
//                        Optional.of("user1@slac.stanford.edu"),
//                        List.of(
//                                LogbookGroupAuthorizationDTO
//                                        .builder()
//                                        .logbookId(newLogbookResult.getPayload())
//                                        .groupId("group-1")
//                                        .authorizationType(Read)
//                                        .build()
//                        )
//                )
//        );
        logbook = assertDoesNotThrow(
                () -> testControllerHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        newLogbookResult.getPayload(),
                        Optional.of(true)
                )
        );

        assertThat(logbook.getPayload())
                .isNotNull();
        assertThat(logbook.getPayload().authorizations())
                .hasSize(2)
                .extracting(LogbookOwnerAuthorizationDTO::owner)
                .contains("user1@slac.stanford.edu", "group-1");

        // remove authorization for all user
//        assertDoesNotThrow(
//                () -> testControllerHelperService.deleteLogbookUsersAuthorizations(
//                        mockMvc,
//                        status().isOk(),
//                        Optional.of("user1@slac.stanford.edu"),
//                        newLogbookResult.getPayload()
//                )
//        );
        logbook = assertDoesNotThrow(
                () -> testControllerHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        newLogbookResult.getPayload(),
                        Optional.of(true)
                )
        );
        assertThat(logbook.getPayload())
                .isNotNull();
        assertThat(logbook.getPayload().authorizations())
                .hasSize(1);

        // remove authorization for all user
//        assertDoesNotThrow(
//                () -> testControllerHelperService.deleteLogbookGroupAuthorization(
//                        mockMvc,
//                        status().isOk(),
//                        Optional.of("user1@slac.stanford.edu"),
//                        newLogbookResult.getPayload()
//                )
//        );
        logbook = assertDoesNotThrow(
                () -> testControllerHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        newLogbookResult.getPayload(),
                        Optional.of(true)
                )
        );
        assertThat(logbook.getPayload())
                .isNotNull();
        assertThat(logbook.getPayload().authorizations())
                .isNullOrEmpty();
    }

    @Test
    public void testMultipleAuthorizationForUserAndGroup() {
        var newLogbookResult = assertDoesNotThrow(
                () -> testControllerHelperService.getNewLogbookWithNameWithAuthorizationAndAppToken(
                        mockMvc,
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        "new logbook",
                        emptyList()
                )
        );

        assertThat(newLogbookResult.getPayload()).isNotEmpty();

        var logbook = assertDoesNotThrow(
                () -> testControllerHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        newLogbookResult.getPayload(),
                        Optional.of(true)
                )
        );

        assertThat(logbook.getPayload())
                .isNotNull();
        assertThat(logbook.getPayload().authorizations())
                .isNullOrEmpty();

        // apply authorization for group
        assertDoesNotThrow(
                () -> testControllerHelperService.applyLogbookUserAuthorization(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        List.of(
                                LogbookUserAuthorizationDTO
                                        .builder()
                                        .logbookId(newLogbookResult.getPayload())
                                        .userId("user1@slac.stanford.edu")
                                        .authorizationType(Write)
                                        .build(),
                                LogbookUserAuthorizationDTO
                                        .builder()
                                        .logbookId(newLogbookResult.getPayload())
                                        .userId("user2@slac.stanford.edu")
                                        .authorizationType(Read)
                                        .build(),
                                LogbookUserAuthorizationDTO
                                        .builder()
                                        .logbookId(newLogbookResult.getPayload())
                                        .userId("user1@slac.stanford.edu")
                                        .authorizationType(Admin)
                                        .build()
                        )
                )
        );
        // apply authorization for group
//        assertDoesNotThrow(
//                () -> testControllerHelperService.applyLogbookGroupAuthorizations(
//                        mockMvc,
//                        status().isCreated(),
//                        Optional.of("user1@slac.stanford.edu"),
//                        List.of(
//                                LogbookGroupAuthorizationDTO
//                                        .builder()
//                                        .logbookId(newLogbookResult.getPayload())
//                                        .groupId("group-1")
//                                        .authorizationType(Read)
//                                        .build(),
//                                LogbookGroupAuthorizationDTO
//                                        .builder()
//                                        .logbookId(newLogbookResult.getPayload())
//                                        .groupId("group-2")
//                                        .authorizationType(Write)
//                                        .build(),
//                                LogbookGroupAuthorizationDTO
//                                        .builder()
//                                        .logbookId(newLogbookResult.getPayload())
//                                        .groupId("group-1")
//                                        .authorizationType(Admin)
//                                        .build()
//                        )
//                )
//        );
        logbook = assertDoesNotThrow(
                () -> testControllerHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        newLogbookResult.getPayload(),
                        Optional.of(true)
                )
        );

        assertThat(logbook.getPayload())
                .isNotNull();
        assertThat(logbook.getPayload().authorizations())
                .hasSize(4)
                .extracting(LogbookOwnerAuthorizationDTO::owner)
                .contains("user1@slac.stanford.edu", "user2@slac.stanford.edu", "group-1", "group-2");
        // check the maximum authorization for each user and group
        assertTrue(
                logbook.getPayload().authorizations().stream()
                        .collect(Collectors.groupingBy(LogbookOwnerAuthorizationDTO::owner))
                        .values()
                        .stream()
                        .allMatch(authorizations -> authorizations.size() == 1)
        );
        logbook.getPayload().authorizations().stream().filter(a -> a.owner().compareTo("user1@slac.stanford.edu") == 0).findFirst().ifPresent(
                a -> assertThat(a.authorizationType()).isEqualTo(Admin)
        );
        logbook.getPayload().authorizations().stream().filter(a -> a.owner().compareTo("user2@slac.stanford.edu") == 0).findFirst().ifPresent(
                a -> assertThat(a.authorizationType()).isEqualTo(Read)
        );
        logbook.getPayload().authorizations().stream().filter(a -> a.owner().compareTo("group-1") == 0).findFirst().ifPresent(
                a -> assertThat(a.authorizationType()).isEqualTo(Admin)
        );
        logbook.getPayload().authorizations().stream().filter(a -> a.owner().compareTo("group-2") == 0).findFirst().ifPresent(
                a -> assertThat(a.authorizationType()).isEqualTo(Write)
        );
    }

    @Test
    public void testFetchUserAuthorization() {
        var logbook1Result = assertDoesNotThrow(
                () -> testControllerHelperService.getNewLogbookWithNameWithAuthorizationAndAppToken(
                        mockMvc,
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        "new logbook",
                        List.of(
                                LogbookOwnerAuthorizationDTO
                                        .builder()
                                        .owner("user1@slac.stanford.edu")
                                        .ownerType(AuthorizationOwnerTypeDTO.User)
                                        .authorizationType(
                                                Admin
                                        )
                                        .build(),
                                LogbookOwnerAuthorizationDTO
                                        .builder()
                                        .owner("user2@slac.stanford.edu")
                                        .ownerType(AuthorizationOwnerTypeDTO.User)
                                        .authorizationType(
                                                Read
                                        )
                                        .build()
                        )
                )
        );
        var logbook2Result = assertDoesNotThrow(
                () -> testControllerHelperService.getNewLogbookWithNameWithAuthorizationAndAppToken(
                        mockMvc,
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        "new logbook 2",
                        List.of(
                                LogbookOwnerAuthorizationDTO
                                        .builder()
                                        .owner("user2@slac.stanford.edu")
                                        .ownerType(AuthorizationOwnerTypeDTO.User)
                                        .authorizationType(
                                                Write
                                        )
                                        .build(),
                                LogbookOwnerAuthorizationDTO
                                        .builder()
                                        .owner("user1@2slac.stanford.edu")
                                        .ownerType(AuthorizationOwnerTypeDTO.User)
                                        .authorizationType(
                                                Read
                                        )
                                        .build(),
                                LogbookOwnerAuthorizationDTO
                                        .builder()
                                        .owner("group-1")
                                        .ownerType(AuthorizationOwnerTypeDTO.Group)
                                        .authorizationType(
                                                Admin
                                        )
                                        .build()
                        )
                )
        );

        var currentUserAuthorization = assertDoesNotThrow(
                () -> testControllerHelperService.getLogbookAuthorizationForCurrentUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu")
                )
        );

        assertThat(currentUserAuthorization.getPayload())
                .hasSize(2)
                .anySatisfy(auth -> AssertionsForClassTypes.assertThat(auth).is(LogbookAuthorizationDTOIs.of(logbook1Result.getPayload(), Admin)))
                .anySatisfy(auth -> AssertionsForClassTypes.assertThat(auth).is(LogbookAuthorizationDTOIs.of(logbook2Result.getPayload(), Admin)));

        currentUserAuthorization = assertDoesNotThrow(
                () -> testControllerHelperService.getLogbookAuthorizationForCurrentUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user2@slac.stanford.edu")
                )
        );

        assertThat(currentUserAuthorization.getPayload())
                .hasSize(2)
                .anySatisfy(auth -> AssertionsForClassTypes.assertThat(auth).is(LogbookAuthorizationDTOIs.of(logbook1Result.getPayload(), Read)))
                .anySatisfy(auth -> AssertionsForClassTypes.assertThat(auth).is(LogbookAuthorizationDTOIs.of(logbook2Result.getPayload(), Write)));
    }

    /**
     * Test to ensure that the authorization is not duplicated when the same authorization is added multiple times
     */
    static private class LogbookAuthorizationDTOIs extends Condition<LogbookAuthorizationDTO> {
        private final String logbookId;
        private final AuthorizationTypeDTO authorizationTypeDTO;

        private LogbookAuthorizationDTOIs(String logbookId, AuthorizationTypeDTO authorizationTypeDTO) {
            this.logbookId = logbookId;
            this.authorizationTypeDTO = authorizationTypeDTO;
        }

        public static LogbookAuthorizationDTOIs of(String logbookId, AuthorizationTypeDTO authorizationTypeDTO) {
            return new LogbookAuthorizationDTOIs(logbookId, authorizationTypeDTO);
        }

        @Override
        public boolean matches(LogbookAuthorizationDTO authorizationDTO) {
            return authorizationDTO != null
                    && authorizationDTO.logbookId().equalsIgnoreCase(logbookId)
                    && authorizationDTO.authorizationType().equals(authorizationTypeDTO);
        }
    }
}
