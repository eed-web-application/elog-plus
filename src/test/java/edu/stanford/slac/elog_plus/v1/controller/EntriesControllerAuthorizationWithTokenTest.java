package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.EntryNewDTO;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.exception.NotAuthorized;
import edu.stanford.slac.elog_plus.model.*;
import edu.stanford.slac.elog_plus.service.AuthService;
import edu.stanford.slac.elog_plus.service.LogbookService;
import edu.stanford.slac.elog_plus.v1.service.DocumentGenerationService;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EntriesControllerAuthorizationWithTokenTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LogbookService logbookService;

    @Autowired
    AppProperties appProperties;

    @Autowired
    private AuthService authService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TestControllerHelperService testControllerHelperService;

    @Autowired
    private DocumentGenerationService documentGenerationService;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Entry.class);
        mongoTemplate.remove(new Query(), Attachment.class);
        mongoTemplate.remove(new Query(), Logbook.class);

        //reset authorizations
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
    }

    @Test
    public void createNewLogSuccessWithAuthTokenOnLogbook() throws Exception {
        var newLogBookResult = assertDoesNotThrow(
                () -> testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                        mockMvc,
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        "LogbookAuthTest1",
                        List.of(
                                AuthorizationDTO
                                        .builder()
                                        .ownerType("User")
                                        .owner("user2@slac.stanford.edu")
                                        .authorizationType("Write")
                                        .build(),
                                AuthorizationDTO
                                        .builder()
                                        .ownerType("User")
                                        .owner("user3@slac.stanford.edu")
                                        .authorizationType("Admin")
                                        .build()
                        )
                )
        );
        assertThat(newLogBookResult.getErrorCode()).isEqualTo(0);
        var newLogIdForWriter =
                assertDoesNotThrow(
                        () -> testControllerHelperService.createNewLog(
                                mockMvc,
                                status().isCreated(),
                                Optional.of(
                                        "user2@slac.stanford.edu"
                                ),
                                EntryNewDTO
                                        .builder()
                                        .logbooks(
                                                List.of(
                                                        newLogBookResult.getPayload()
                                                )
                                        )
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );

        assertThat(newLogIdForWriter.getErrorCode()).isEqualTo(0);
        var newLogIdForAdmin =
                assertDoesNotThrow(
                        () -> testControllerHelperService.createNewLog(
                                mockMvc,
                                status().isCreated(),
                                Optional.of(
                                        "user3@slac.stanford.edu"
                                ),
                                EntryNewDTO
                                        .builder()
                                        .logbooks(
                                                List.of(
                                                        newLogBookResult.getPayload()
                                                )
                                        )
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );

        assertThat(newLogIdForWriter.getErrorCode()).isEqualTo(0);
    }

    @Test
    public void createNewLogSuccessWithWriterGroupAuth() throws Exception {
        var newLogBookResult = assertDoesNotThrow(
                () -> testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                        mockMvc,
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        "LogbookAuthTest1",
                        List.of(
                                AuthorizationDTO
                                        .builder()
                                        .ownerType("Group")
                                        .owner("group-2")
                                        .authorizationType("Write")
                                        .build()
                        )
                )
        );
        assertThat(newLogBookResult.getErrorCode()).isEqualTo(0);
        var newLogIdForWriter =
                assertDoesNotThrow(
                        () -> testControllerHelperService.createNewLog(
                                mockMvc,
                                status().isCreated(),
                                Optional.of(
                                        "user2@slac.stanford.edu"
                                ),
                                EntryNewDTO
                                        .builder()
                                        .logbooks(
                                                List.of(
                                                        newLogBookResult.getPayload()
                                                )
                                        )
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );

        assertThat(newLogIdForWriter.getErrorCode()).isEqualTo(0);
    }

    @Test
    public void createNewLogAcrossLogbookAndCheckAuthOnlyOnOne() throws Exception {
        var newLogBookResult1 = assertDoesNotThrow(
                () -> testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                        mockMvc,
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        "LogbookAuthTest1",
                        List.of(
                                AuthorizationDTO
                                        .builder()
                                        .ownerType("User")
                                        .owner("user2@slac.stanford.edu")
                                        .authorizationType("Read")
                                        .build()
                        )
                )
        );
        assertThat(newLogBookResult1.getErrorCode()).isEqualTo(0);
        var newLogBookResult2 = assertDoesNotThrow(
                () -> testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                        mockMvc,
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        "LogbookAuthTest2",
                        List.of(
                                AuthorizationDTO
                                        .builder()
                                        .ownerType("User")
                                        .owner("user3@slac.stanford.edu")
                                        .authorizationType("Read")
                                        .build()
                        )
                )
        );
        assertThat(newLogBookResult2.getErrorCode()).isEqualTo(0);
        ApiResultResponse<String> newLogID =
                assertDoesNotThrow(
                        () -> testControllerHelperService.createNewLog(
                                mockMvc,
                                status().isCreated(),
                                Optional.of(
                                        "user1@slac.stanford.edu"
                                ),
                                EntryNewDTO
                                        .builder()
                                        .logbooks(
                                                List.of(
                                                        newLogBookResult1.getPayload(),
                                                        newLogBookResult2.getPayload()
                                                )
                                        )
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );

        assertThat(newLogID.getErrorCode()).isEqualTo(0);

        //fetch the entry with user that can read only from on logbook
        var entryForUser2 = testControllerHelperService.getFullLog(
                mockMvc,
                Optional.of(
                        "user2@slac.stanford.edu"
                ),
                newLogID.getPayload()
        );
        assertThat(entryForUser2.getErrorCode()).isEqualTo(0);
        assertThat(entryForUser2.getPayload().logbooks()).extracting("id").contains(newLogBookResult1.getPayload());

        var entryForUser3 = testControllerHelperService.getFullLog(
                mockMvc,
                Optional.of(
                        "user3@slac.stanford.edu"
                ),
                newLogID.getPayload()
        );
        assertThat(entryForUser3.getErrorCode()).isEqualTo(0);
        assertThat(entryForUser3.getPayload().logbooks()).extracting("id").contains(newLogBookResult2.getPayload());
    }

}
