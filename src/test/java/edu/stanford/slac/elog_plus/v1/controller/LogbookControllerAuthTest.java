package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.AuthenticationTokenDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.model.AuthenticationToken;
import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.AuthService;
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

import static edu.stanford.slac.elog_plus.api.v1.dto.AuthorizationTypeDTO.Read;
import static edu.stanford.slac.elog_plus.api.v1.dto.AuthorizationTypeDTO.Write;
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
                                .ownerType("User")
                                .authorizationType(
                                        Write
                                )
                                .build(),
                        AuthorizationDTO
                                .builder()
                                .owner("user3@slac.stanford.edu")
                                .ownerType("User")
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
                                .ownerType("User")
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
                                .ownerType("User")
                                .authorizationType(
                                        Write
                                )
                                .build(),
                        AuthorizationDTO
                                .builder()
                                .owner(testControllerHelperService.getTokenEmailForLogbookToken("token-a", "new logbook"))
                                .ownerType("Application")
                                .authorizationType(
                                        Read
                                )
                                .build()
                ),
                List.of(
                        AuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
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
                                .ownerType("User")
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
}
