package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthenticationTokenDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
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
public class LogbookControllerAuthWithAuthenticationTokenTest {
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
        var newLogbookApiResultOne = testControllerHelperService.getNewLogbookWithNameWithAuthorizationAndAppToken(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook",
                List.of(
                        AuthorizationDTO
                                .builder()
                                .owner(testControllerHelperService.getTokenEmailForLogbookToken("token-a", "new logbook"))
                                .ownerType(AuthorizationOwnerTypeDTO.Token)
                                .authorizationType(
                                        Write
                                )
                                .build(),
                        AuthorizationDTO
                                .builder()
                                .owner(testControllerHelperService.getTokenEmailForLogbookToken("token-b", "new logbook"))
                                .ownerType(AuthorizationOwnerTypeDTO.Token)
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
                                .build(),
                        AuthenticationTokenDTO
                                .builder()
                                .name("token-b")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        var newLogbookApiResultTwo = testControllerHelperService.getNewLogbookWithNameWithAuthorizationAndAppToken(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook two",
                List.of(
                        AuthorizationDTO
                                .builder()
                                .owner(testControllerHelperService.getTokenEmailForLogbookToken("token-a", "new logbook two"))
                                .ownerType(AuthorizationOwnerTypeDTO.Token)
                                .authorizationType(
                                        Write
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
        var allLogbookResultTokenA = assertDoesNotThrow(
                () -> testControllerHelperService.getAllLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("token-a@new-logbook.elog.slac.app$"),
                        Optional.of(false),
                        Optional.empty()
                )
        );

        assertThat(allLogbookResultTokenA)
                .isNotNull()
                .extracting(
                        ApiResultResponse::getErrorCode
                )
                .isEqualTo(0);
        assertThat(allLogbookResultTokenA.getPayload())
                .hasSize(1)
                .extracting(LogbookDTO::id)
                .contains(
                        newLogbookApiResultOne.getPayload()
                );

        var allLogbookResultTokenB = assertDoesNotThrow(
                () -> testControllerHelperService.getAllLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("token-a@new-logbook-two.elog.slac.app$"),
                        Optional.of(false),
                        Optional.empty()
                )
        );

        assertThat(allLogbookResultTokenB)
                .isNotNull()
                .extracting(
                        ApiResultResponse::getErrorCode
                )
                .isEqualTo(0);
        assertThat(allLogbookResultTokenB.getPayload())
                .hasSize(1)
                .extracting(LogbookDTO::id)
                .contains(
                        newLogbookApiResultTwo.getPayload()
                );
    }

}
