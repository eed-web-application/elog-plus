package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
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
public class LogbookControllerAuthWithGlobalAuthenticationTokenTest {
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
        // create token
        ApiResultResponse<AuthenticationTokenDTO> tokenResult = assertDoesNotThrow(
                () ->
                        testControllerHelperService.createNewAuthenticationToken(
                                mockMvc,
                                status().isCreated(),
                                Optional.of(
                                        "user1@slac.stanford.edu"
                                ),
                                NewAuthenticationTokenDTO
                                        .builder()
                                        .name("global-token-a")
                                        .expiration(LocalDate.of(2023,12,31))
                                        .build()
                        )
        );
        assertThat(tokenResult.getErrorCode()).isEqualTo(0);
        assertThat(tokenResult.getPayload()).extracting(AuthenticationTokenDTO::name).isEqualTo("global-token-a");
        assertThat(tokenResult.getPayload()).extracting(AuthenticationTokenDTO::email).isEqualTo("global-token-a@%s".formatted(appProperties.getApplicationTokenDomain()));
        assertThat(tokenResult.getPayload()).extracting(AuthenticationTokenDTO::token).isNotNull();

        //authorize token to a logbook
        var newLogbookApiResultOne = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook",
                List.of(
                        AuthorizationDTO
                                .builder()
                                .owner(tokenResult.getPayload().email())
                                .ownerType("Application")
                                .authorizationType(
                                        Write
                                )
                                .build()
                )
        );
        // creaae new logbook for another user
        var newLogbookApiResultTwo = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook two",
                List.of(
                        AuthorizationDTO
                                .builder()
                                .owner("user2@slac.stanford.edu")
                                .ownerType("User")
                                .authorizationType(
                                        Write
                                )
                                .build()
                )
        );
        var allLogbookResultTokenA = assertDoesNotThrow(
                () -> testControllerHelperService.getAllLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of(tokenResult.getPayload().email()),
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
    }

}
