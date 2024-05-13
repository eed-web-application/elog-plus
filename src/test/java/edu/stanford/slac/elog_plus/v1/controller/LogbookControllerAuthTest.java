package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.*;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.config.ELOGAppProperties;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
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

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Read;
import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Write;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
                        AuthorizationDTO
                                .builder()
                                .owner("user2@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .authorizationType(
                                        Write
                                )
                                .build(),
                        AuthorizationDTO
                                .builder()
                                .owner("user3@slac.stanford.edu")
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
                        AuthorizationDTO
                                .builder()
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .owner("user2@slac.stanford.edu")
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
        var tokensEmail = testControllerHelperService.createTokens(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                List.of(
                        NewAuthenticationTokenDTO
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
                        AuthorizationDTO
                                .builder()
                                .owner("user2@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .authorizationType(
                                        Write
                                )
                                .build(),
                        AuthorizationDTO
                                .builder()
                                .owner(tokensEmail.getFirst())
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
                        AuthorizationDTO
                                .builder()
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .owner("user2@slac.stanford.edu")
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
                                AuthorizationDTO
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
                                AuthorizationDTO
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
                                AuthorizationDTO
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
    }
}
