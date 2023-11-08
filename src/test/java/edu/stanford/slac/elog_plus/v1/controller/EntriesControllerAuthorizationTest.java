package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.exception.LogbookNotAuthorized;
import edu.stanford.slac.elog_plus.exception.NotAuthorized;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.AuthService;
import edu.stanford.slac.elog_plus.service.LogbookService;
import edu.stanford.slac.elog_plus.v1.service.DocumentGenerationService;
import org.assertj.core.api.AssertionsForClassTypes;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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
public class EntriesControllerAuthorizationTest {
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
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
    }

    @Test
    public void createNewLogUnauthorizedWriteFails() throws Exception {
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
                                        .authorizationType(AuthorizationTypeDTO.Read)
                                        .build()
                        )
                )
        );
        assertThat(newLogBookResult.getErrorCode()).isEqualTo(0);
        NotAuthorized unauthorizedWriteOnLogbookByReader =
                assertThrows(
                        NotAuthorized.class,
                        () -> testControllerHelperService.createNewLog(
                                mockMvc,
                                status().isUnauthorized(),
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

        assertThat(unauthorizedWriteOnLogbookByReader.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createNewLogUnauthorizedUnmappedUserToLogbook() throws Exception {
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
                                        .authorizationType(AuthorizationTypeDTO.Read)
                                        .build()
                        )
                )
        );
        assertThat(newLogBookResult.getErrorCode()).isEqualTo(0);
        NotAuthorized unauthorizedWriteOnLogbookByReader =
                assertThrows(
                        NotAuthorized.class,
                        () -> testControllerHelperService.createNewLog(
                                mockMvc,
                                status().isUnauthorized(),
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

        assertThat(unauthorizedWriteOnLogbookByReader.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createNewLogSuccessWithWriterUserAndAdmin() throws Exception {
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
                                        .authorizationType(AuthorizationTypeDTO.Write)
                                        .build(),
                                AuthorizationDTO
                                        .builder()
                                        .ownerType("User")
                                        .owner("user3@slac.stanford.edu")
                                        .authorizationType(AuthorizationTypeDTO.Admin)
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
                                        .authorizationType(AuthorizationTypeDTO.Write)
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
                                        .authorizationType(AuthorizationTypeDTO.Read)
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
                                        .authorizationType(AuthorizationTypeDTO.Read)
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

    @Test
    public void testEntriesSearchForAuthorization() {
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
                                        .authorizationType(AuthorizationTypeDTO.Read)
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
                                        .authorizationType(AuthorizationTypeDTO.Read)
                                        .build()
                        )
                )
        );

        // add one entry into first logbook
        ApiResultResponse<String> newLogID1 =
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
                                                        newLogBookResult1.getPayload()
                                                )
                                        )
                                        .text("This is a log for test")
                                        .title("A very wonderful log for logbook1")
                                        .build()
                        )
                );

        assertThat(newLogID1.getErrorCode()).isEqualTo(0);

        // add two entry into second logbook
        ApiResultResponse<String> newLogID2 =
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
                                        .title("A very wonderful logbook 1 and 2")
                                        .build()
                        )
                );

        assertThat(newLogID2.getErrorCode()).isEqualTo(0);

        ApiResultResponse<String> newLogID3 =
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
                                        .title("Another very wonderful logbook 1 and 2")
                                        .build()
                        )
                );

        assertThat(newLogID3.getErrorCode()).isEqualTo(0);

        //execute search with user two
        var searchResult = assertDoesNotThrow(
                () -> testControllerHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.of(
                                "user2@slac.stanford.edu"
                        ),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(List.of(newLogBookResult1.getPayload())),
                        Optional.empty()
                )
        );
        assertThat(searchResult).isNotNull();
        assertThat(searchResult.getErrorCode()).isEqualTo(0);
        assertThat(searchResult.getPayload()).hasSize(3);
        assertThat(searchResult.getPayload())
                .extracting(EntrySummaryDTO::id)
                .contains(
                        newLogID1.getPayload(),
                        newLogID2.getPayload(),
                        newLogID3.getPayload()
                );

        // search same user on al the authorized logbook
        searchResult = assertDoesNotThrow(
                () -> testControllerHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.of(
                                "user2@slac.stanford.edu"
                        ),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        assertThat(searchResult).isNotNull();
        assertThat(searchResult.getErrorCode()).isEqualTo(0);
        assertThat(searchResult.getPayload()).hasSize(3);
        assertThat(searchResult.getPayload())
                .extracting(EntrySummaryDTO::id)
                .contains(
                        newLogID1.getPayload(),
                        newLogID2.getPayload(),
                        newLogID3.getPayload()
                );

        //execute search with user three
        searchResult = assertDoesNotThrow(
                () -> testControllerHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.of(
                                "user3@slac.stanford.edu"
                        ),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(List.of(newLogBookResult2.getPayload())),
                        Optional.empty()
                )
        );
        assertThat(searchResult).isNotNull();
        assertThat(searchResult.getErrorCode()).isEqualTo(0);
        assertThat(searchResult.getPayload()).hasSize(2);
        assertThat(searchResult.getPayload())
                .extracting(EntrySummaryDTO::id)
                .contains(
                        newLogID2.getPayload(),
                        newLogID3.getPayload()
                );
    }

    @Test
    public void searchFailsOnNonAuthorizedLogbook() {
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
                                        .authorizationType(AuthorizationTypeDTO.Read)
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
                                        .authorizationType(AuthorizationTypeDTO.Read)
                                        .build()
                        )
                )
        );


        // this should give exception because user 2 is not authorized on logbook 2
        var exceptionForUser2 = assertThrows(
                LogbookNotAuthorized.class,
                () -> testControllerHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isUnauthorized(),
                        Optional.of(
                                "user2@slac.stanford.edu"
                        ),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(List.of(newLogBookResult2.getPayload())),
                        Optional.empty()
                )
        );
        assertThat(exceptionForUser2.getErrorCode()).isEqualTo(-1);
        assertThat(exceptionForUser2.getErrorMessage()).contains("logbookauthtest2");


        // this should give exception because user 3 is not authorized on logbook 1
        var exceptionForUser3 = assertThrows(
                LogbookNotAuthorized.class,
                () -> testControllerHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isUnauthorized(),
                        Optional.of(
                                "user3@slac.stanford.edu"
                        ),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(List.of(newLogBookResult1.getPayload())),
                        Optional.empty()
                )
        );
        assertThat(exceptionForUser3.getErrorCode()).isEqualTo(-1);
        assertThat(exceptionForUser3.getErrorMessage()).contains("logbookauthtest1");
    }
}
