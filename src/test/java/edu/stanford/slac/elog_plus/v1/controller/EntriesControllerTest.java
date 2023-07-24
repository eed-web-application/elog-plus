package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.exception.EntryNotFound;
import edu.stanford.slac.elog_plus.exception.SupersedeAlreadyCreated;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.model.Tag;
import edu.stanford.slac.elog_plus.service.LogbookService;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class EntriesControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    LogbookService logbookService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TestHelperService testHelperService;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Entry.class);
        mongoTemplate.remove(new Query(), Attachment.class);
        mongoTemplate.remove(new Query(), Logbook.class);
    }

    @Test
    public void createNewLog() throws Exception {
        var newLogBookResult = getTestLogbook();
        ApiResultResponse<String> newLogID =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().isCreated(),
                                        EntryNewDTO
                                                .builder()
                                                .logbook(newLogBookResult.getPayload().name())
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .build()
                                )
                );

        AssertionsForClassTypes.assertThat(newLogID).isNotNull();
        AssertionsForClassTypes.assertThat(newLogID.getErrorCode()).isEqualTo(0);
    }

    @Test
    public void createNewLogWitTag() throws Exception {
        var newLogBookResult = getTestLogbook();

        ApiResultResponse<String> newTagID = assertDoesNotThrow(
                () ->
                        testHelperService.createNewLogbookTags(
                                mockMvc,
                                status().isCreated(),
                                newLogBookResult.getPayload().id(),
                                NewTagDTO
                                        .builder()
                                        .name("TaG 2")
                                        .build()
                        )
        );

        ApiResultResponse<String> newLogID =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().isCreated(),
                                        EntryNewDTO
                                                .builder()
                                                .logbook(newLogBookResult.getPayload().name())
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .tags(
                                                        List.of(
                                                                "tag-1",
                                                                "TaG 2",
                                                                "tag-3"
                                                        )
                                                )
                                                .build()
                                )
                );

        assertThat(newLogID).isNotNull();
        assertThat(newLogID.getErrorCode()).isEqualTo(0);

        ApiResultResponse<List<TagDTO>> allTags = assertDoesNotThrow(
                () ->
                        testHelperService.getLogbookTags(
                                mockMvc,
                                status().isOk(),
                                newLogBookResult.getPayload().id()
                        )
        );
        assertThat(allTags).isNotNull();
        assertThat(allTags.getErrorCode()).isEqualTo(0);
        assertThat(allTags.getPayload())
                .hasSize(3)
                .extracting("name")
                .contains("tag-1", "tag-2", "tag-3");
    }

    @Test
    public void createNewLogAndSearchWithPaging() throws Exception {
        var newLogBookResult = getTestLogbook();
        ApiResultResponse<String> newLogID =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().isCreated(),
                                        EntryNewDTO
                                                .builder()
                                                .logbook(newLogBookResult.getPayload().name())
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .build()
                                )
                );

        AssertionsForClassTypes.assertThat(newLogID).isNotNull();
        AssertionsForClassTypes.assertThat(newLogID.getErrorCode()).isEqualTo(0);

        var queryResult = testHelperService.submitSearchByGetWithAnchor(
                mockMvc,
                status().isOk(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(10),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
        AssertionsForClassTypes.assertThat(queryResult.getPayload().size()).isEqualTo(1);
    }

    @Test
    public void fetchInvalidLogbook() {
        EntryNotFound newLogID =
                assertThrows(
                        EntryNotFound.class,
                        () ->
                                testHelperService.getFullLog(
                                        mockMvc,
                                        status().is4xxClientError(),
                                        "bad-id"
                                )
                );
        AssertionsForClassTypes.assertThat(newLogID.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void fetchFullLog() {
        var newLogBookResult = getTestLogbook();
        ApiResultResponse<String> newLogID =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().isCreated(),
                                        EntryNewDTO
                                                .builder()
                                                .logbook(newLogBookResult.getPayload().name())
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .build()
                                )
                );
        AssertionsForClassTypes.assertThat(newLogID.getErrorCode()).isEqualTo(0);

        ApiResultResponse<EntryDTO> fullLog = assertDoesNotThrow(
                () ->
                        testHelperService.getFullLog(
                                mockMvc,
                                status().isOk(),
                                newLogID.getPayload()
                        )
        );
        AssertionsForClassTypes.assertThat(newLogID.getErrorCode()).isEqualTo(0);
        AssertionsForClassTypes.assertThat(fullLog.getPayload().id()).isEqualTo(newLogID.getPayload());
    }

    @Test
    public void createNewSupersedeLog() throws Exception {
        var newLogBookResult = getTestLogbook();
        ApiResultResponse<String> newLogIDResult =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().isCreated(),
                                        EntryNewDTO
                                                .builder()
                                                .logbook(newLogBookResult.getPayload().name())
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .build()
                                )
                );

        AssertionsForClassTypes.assertThat(newLogIDResult).isNotNull();
        AssertionsForClassTypes.assertThat(newLogIDResult.getErrorCode()).isEqualTo(0);
        //create supersede
        ApiResultResponse<String> newSupersedeLogIDResult = assertDoesNotThrow(
                () -> testHelperService.createNewSupersedeLog(
                        mockMvc,
                        status().isCreated(),
                        newLogIDResult.getPayload(),
                        EntryNewDTO
                                .builder()
                                .logbook(newLogBookResult.getPayload().name())
                                .text("This is a log for test")
                                .title("A very wonderful supersede log")
                                .build()
                )
        );

        //check old log for supersede info
        ApiResultResponse<EntryDTO> oldFull = assertDoesNotThrow(
                () ->
                        testHelperService.getFullLog(
                                mockMvc,
                                status().isOk(),
                                newLogIDResult.getPayload()
                        )
        );

        assertThat(oldFull.getErrorCode()).isEqualTo(0);
        assertThat(oldFull.getPayload().supersedeBy()).isEqualTo(newSupersedeLogIDResult.getPayload());

        // the search api now should return only the new log and not the superseded on
        var queryResult = testHelperService.submitSearchByGetWithAnchor(
                mockMvc,
                status().isOk(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(10),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
        AssertionsForClassTypes.assertThat(queryResult).isNotNull();
        AssertionsForClassTypes.assertThat(queryResult.getPayload().size()).isEqualTo(1);
    }

    @Test
    public void createDoubleSupersedeFailed() throws Exception {
        var newLogBookResult = getTestLogbook();
        ApiResultResponse<String> newLogIDResult =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().isCreated(),
                                        EntryNewDTO
                                                .builder()
                                                .logbook(newLogBookResult.getPayload().name())
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .build()
                                )
                );

        AssertionsForClassTypes.assertThat(newLogIDResult).isNotNull();
        AssertionsForClassTypes.assertThat(newLogIDResult.getErrorCode()).isEqualTo(0);
        //create supersede
        ApiResultResponse<String> newSupersedeLogIDResult = assertDoesNotThrow(
                () -> testHelperService.createNewSupersedeLog(
                        mockMvc,
                        status().isCreated(),
                        newLogIDResult.getPayload(),
                        EntryNewDTO
                                .builder()
                                .logbook(newLogBookResult.getPayload().name())
                                .text("This is a log for test")
                                .title("A very wonderful supersede log")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(newSupersedeLogIDResult).isNotNull();
        AssertionsForClassTypes.assertThat(newSupersedeLogIDResult.getErrorCode()).isEqualTo(0);

        SupersedeAlreadyCreated exception = assertThrows(
                SupersedeAlreadyCreated.class,
                () -> testHelperService.createNewSupersedeLog(
                        mockMvc,
                        status().is4xxClientError(),
                        newLogIDResult.getPayload(),
                        EntryNewDTO
                                .builder()
                                .logbook(newLogBookResult.getPayload().name())
                                .text("This is a log for test")
                                .title("A very wonderful supersede log")
                                .build()
                )
        );
        assertThat(exception.getErrorCode()).isEqualTo(-3);
    }

    @Test
    public void createSupersedeFailedOnNotFoundEntry() throws Exception {
        var newLogBookResult = getTestLogbook();
        //create supersede
        EntryNotFound exception = assertThrows(
                EntryNotFound.class,
                () -> testHelperService.createNewSupersedeLog(
                        mockMvc,
                        status().is4xxClientError(),
                        "not found superseded log",
                        EntryNewDTO
                                .builder()
                                .logbook(newLogBookResult.getPayload().name())
                                .text("This is a log for test")
                                .title("A very wonderful supersede log")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(exception.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void getLogHistoryAndFollowingLog() throws Exception {
        var newLogBookResult = getTestLogbook();
        ApiResultResponse<String> newLogIDResult =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().isCreated(),
                                        EntryNewDTO
                                                .builder()
                                                .logbook(newLogBookResult.getPayload().name())
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .build()
                                )
                );

        AssertionsForClassTypes.assertThat(newLogIDResult).isNotNull();
        AssertionsForClassTypes.assertThat(newLogIDResult.getErrorCode()).isEqualTo(0);
        //create supersede
        ApiResultResponse<String> newSupersedeLogIDResult1 = assertDoesNotThrow(
                () -> testHelperService.createNewSupersedeLog(
                        mockMvc,
                        status().isCreated(),
                        newLogIDResult.getPayload(),
                        EntryNewDTO
                                .builder()
                                .logbook(newLogBookResult.getPayload().name())
                                .text("This is a log for test")
                                .title("A very wonderful supersede log")
                                .build()
                )
        );

        //check old log for supersede info
        ApiResultResponse<String> newSupersedeLogIDResult2 = assertDoesNotThrow(
                () -> testHelperService.createNewSupersedeLog(
                        mockMvc,
                        status().isCreated(),
                        newSupersedeLogIDResult1.getPayload(),
                        EntryNewDTO
                                .builder()
                                .logbook(newLogBookResult.getPayload().name())
                                .text("This is a log for test")
                                .title("A very wonderful supersede log of supersede")
                                .build()
                )
        );

        //return full log with only history
        ApiResultResponse<EntryDTO> fullLog = assertDoesNotThrow(
                () -> testHelperService.getFullLog(
                        mockMvc,
                        status().isOk(),
                        newSupersedeLogIDResult2.getPayload(),
                        false,
                        false,
                        true
                )
        );

        assertThat(fullLog.getErrorCode()).isEqualTo(0);
        assertThat(fullLog.getPayload().history()).extracting("id").containsExactly(
                newSupersedeLogIDResult1.getPayload(),
                newLogIDResult.getPayload()
        );
    }


    @Test
    public void createNewFollowUpLogsAndFetch() throws Exception {
        var newLogBookResult = getTestLogbook();
        ApiResultResponse<String> newLogIDResult =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().isCreated(),
                                        EntryNewDTO
                                                .builder()
                                                .logbook(newLogBookResult.getPayload().name())
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .build()
                                )
                );

        AssertionsForClassTypes.assertThat(newLogIDResult).isNotNull();
        AssertionsForClassTypes.assertThat(newLogIDResult.getErrorCode()).isEqualTo(0);

        //create follow-up
        ApiResultResponse<String> newFULogIDOneResult = assertDoesNotThrow(
                () -> testHelperService.createNewFollowUpLog(
                        mockMvc,
                        status().isCreated(),
                        newLogIDResult.getPayload(),
                        EntryNewDTO
                                .builder()
                                .logbook(newLogBookResult.getPayload().name())
                                .text("This is a log for test")
                                .title("A very wonderful followup log one")
                                .build()
                )
        );

        //create follow-up
        ApiResultResponse<String> newFULogIDTwoResult = assertDoesNotThrow(
                () -> testHelperService.createNewFollowUpLog(
                        mockMvc,
                        status().isCreated(),
                        newLogIDResult.getPayload(),
                        EntryNewDTO
                                .builder()
                                .logbook(newLogBookResult.getPayload().name())
                                .text("This is a log for test")
                                .title("A very wonderful followup log two")
                                .build()
                )
        );

        ApiResultResponse<List<EntrySummaryDTO>> foundFollowUp = assertDoesNotThrow(
                () -> testHelperService.getAllFollowUpLog(
                        mockMvc,
                        status().isOk(),
                        newLogIDResult.getPayload()
                )
        );
        assertThat(foundFollowUp.getErrorCode()).isEqualTo(0);
        assertThat(foundFollowUp.getPayload().size()).isEqualTo(2);

        //get full log without followUPs
        ApiResultResponse<EntryDTO> fullLog = assertDoesNotThrow(
                () -> testHelperService.getFullLog(
                        mockMvc,
                        status().isOk(),
                        newLogIDResult.getPayload(),
                        false
                )
        );

        assertThat(fullLog.getErrorCode()).isEqualTo(0);
        assertThat(fullLog.getPayload().followUp()).isNull();

        //get full log without followUPs
        ApiResultResponse<EntryDTO> fullLogWitFollowUps = assertDoesNotThrow(
                () -> testHelperService.getFullLog(
                        mockMvc,
                        status().isOk(),
                        newLogIDResult.getPayload(),
                        true
                )
        );

        assertThat(fullLogWitFollowUps.getErrorCode()).isEqualTo(0);
        assertThat(fullLogWitFollowUps.getPayload().followUp()).isNotNull();
        assertThat(fullLogWitFollowUps.getPayload().followUp().size()).isEqualTo(2);

        // check for full log with the following up one
        ApiResultResponse<EntryDTO> fullLogWithFollowingUp = assertDoesNotThrow(
                () -> testHelperService.getFullLog(
                        mockMvc,
                        status().isOk(),
                        newFULogIDOneResult.getPayload(),
                        false,
                        true,
                        false
                )
        );

        assertThat(fullLogWithFollowingUp.getErrorCode()).isEqualTo(0);
        assertThat(fullLogWithFollowingUp.getPayload().followingUp()).isNotNull();
        assertThat(fullLogWithFollowingUp.getPayload().followingUp().id()).isEqualTo(newLogIDResult.getPayload());
    }


    @Test
    public void searchWithAnchor() {
        var newLogBookResult = getTestLogbook();
        // create some data
        for (int idx = 0; idx < 100; idx++) {
            int finalIdx = idx;
            ApiResultResponse<String> newLogID =
                    assertDoesNotThrow(
                            () -> testHelperService.createNewLog(
                                    mockMvc,
                                    status().isCreated(),
                                    EntryNewDTO
                                            .builder()
                                            .logbook(newLogBookResult.getPayload().name())
                                            .text("This is a log for test")
                                            .title("A very wonderful log")
                                            .segment(String.valueOf(finalIdx))
                                            .build()
                            )
                    );
            AssertionsForClassTypes.assertThat(newLogID).isNotNull();
        }

        // initial search
        ApiResultResponse<List<EntrySummaryDTO>> firstPageResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        AssertionsForInterfaceTypes.assertThat(firstPageResult.getErrorCode()).isEqualTo(0);
        List<EntrySummaryDTO> firstPage = firstPageResult.getPayload();
        AssertionsForInterfaceTypes.assertThat(firstPage).isNotNull();
        AssertionsForClassTypes.assertThat(firstPage.size()).isEqualTo(10);
        String lastSegment = null;
        for (EntrySummaryDTO l :
                firstPage) {
            if (lastSegment == null) {
                lastSegment = l.segment();
                continue;
            }
            AssertionsForClassTypes.assertThat(Integer.valueOf(lastSegment)).isGreaterThan(
                    Integer.valueOf(l.segment())
            );
            lastSegment = l.segment();
        }
        var testAnchorDate = firstPage.get(firstPage.size() - 1).loggedAt();
        // load next page
        ApiResultResponse<List<EntrySummaryDTO>> nextPageResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.empty(),
                        Optional.of(testAnchorDate),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        AssertionsForInterfaceTypes.assertThat(nextPageResult.getErrorCode()).isEqualTo(0);
        List<EntrySummaryDTO> nextPage = nextPageResult.getPayload();
        AssertionsForInterfaceTypes.assertThat(nextPage).isNotNull();
        AssertionsForClassTypes.assertThat(nextPage.size()).isEqualTo(10);
        // check that the first of next page is not the last of the previous
        AssertionsForClassTypes.assertThat(nextPage.get(0).id()).isNotEqualTo(firstPage.get(firstPage.size() - 1).id());

        lastSegment = nextPage.get(0).segment();
        nextPage.remove(0);
        for (EntrySummaryDTO l :
                nextPage) {
            AssertionsForClassTypes.assertThat(Integer.valueOf(lastSegment)).isGreaterThan(
                    Integer.valueOf(l.segment())
            );
            lastSegment = l.segment();
        }

        // now get all the record upside and downside
        ApiResultResponse<List<EntrySummaryDTO>> prevPageByPinResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.empty(),
                        Optional.of(testAnchorDate),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        AssertionsForInterfaceTypes.assertThat(prevPageByPinResult.getErrorCode()).isEqualTo(0);
        List<EntrySummaryDTO> prevPageByPin = prevPageByPinResult.getPayload();
        AssertionsForInterfaceTypes.assertThat(prevPageByPin).isNotNull();
        AssertionsForClassTypes.assertThat(prevPageByPin.size()).isEqualTo(10);
        AssertionsForInterfaceTypes.assertThat(prevPageByPin).isEqualTo(firstPage);

        // test prev and next
        ApiResultResponse<List<EntrySummaryDTO>> prevAndNextPageByPinResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.empty(),
                        Optional.of(testAnchorDate),
                        Optional.of(10),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        AssertionsForInterfaceTypes.assertThat(prevAndNextPageByPinResult.getErrorCode()).isEqualTo(0);
        List<EntrySummaryDTO> prevAndNextPageByPin = prevAndNextPageByPinResult.getPayload();
        AssertionsForInterfaceTypes.assertThat(prevAndNextPageByPin).isNotNull();
        AssertionsForClassTypes.assertThat(prevAndNextPageByPin.size()).isEqualTo(20);
        AssertionsForInterfaceTypes.assertThat(prevAndNextPageByPin).containsAll(firstPage);
        AssertionsForInterfaceTypes.assertThat(prevAndNextPageByPin).containsAll(nextPage);
    }

    @Test
    public void searchWithTags() {
        ApiResultResponse<LogbookDTO> logbookCreationResult = getTestLogbook();
        // create new logbook
        // create some data
        for (int idx = 0; idx < 100; idx++) {
            int finalIdx = idx;
            ApiResultResponse<String> newTagID = assertDoesNotThrow(
                    () -> testHelperService.createNewLogbookTags(
                            mockMvc,
                            status().isCreated(),
                            logbookCreationResult.getPayload().id(),
                            NewTagDTO
                                    .builder()
                                    .name(String.valueOf(finalIdx))
                                    .build()
                    ));
            assertThat(newTagID).isNotNull();
            assertThat(newTagID.getErrorCode()).isEqualTo(0);

            newTagID = assertDoesNotThrow(
                    () -> testHelperService.createNewLogbookTags(
                            mockMvc,
                            status().isCreated(),
                            logbookCreationResult.getPayload().id(),
                            NewTagDTO
                                    .builder()
                                    .name(String.valueOf("tags-" + (100 + finalIdx)))
                                    .build()
                    ));
            assertThat(newTagID).isNotNull();
            assertThat(newTagID.getErrorCode()).isEqualTo(0);

            ApiResultResponse<String> newLogID =
                    assertDoesNotThrow(
                            () -> testHelperService.createNewLog(
                                    mockMvc,
                                    status().isCreated(),
                                    EntryNewDTO
                                            .builder()
                                            .logbook(logbookCreationResult.getPayload().name())
                                            .text("This is a log for test")
                                            .title("A very wonderful log")
                                            .segment(String.valueOf(finalIdx))
                                            .tags(List.of(String.valueOf(finalIdx), "tags-" + (100 + finalIdx)))
                                            .build()
                            )
                    );
            AssertionsForClassTypes.assertThat(newLogID).isNotNull();
        }

        // initial search
        ApiResultResponse<List<EntrySummaryDTO>> findTags = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.of(List.of("99", "49", "0")),
                        Optional.empty()
                )
        );
        assertThat(findTags).isNotNull();
        assertThat(findTags.getPayload().size()).isEqualTo(3);
        assertThat(findTags.getPayload().get(0).tags()).contains("99");
        assertThat(findTags.getPayload().get(1).tags()).contains("49");
        assertThat(findTags.getPayload().get(2).tags()).contains("0");

        findTags = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.of(List.of("99", "49", "tags-100")),
                        Optional.empty()
                )
        );
        AssertionsForInterfaceTypes.assertThat(findTags).isNotNull();
        assertThat(findTags.getPayload().size()).isEqualTo(3);
        assertThat(findTags.getPayload().get(0).tags()).contains("99");
        assertThat(findTags.getPayload().get(1).tags()).contains("49");
        assertThat(findTags.getPayload().get(2).tags()).contains("0");
    }

    private ApiResultResponse<LogbookDTO> getTestLogbook() {
        ApiResultResponse<String> logbookCreationResult = assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO
                                .builder()
                                .name(UUID.randomUUID().toString())
                                .build()
                ));
        assertThat(logbookCreationResult).isNotNull();
        assertThat(logbookCreationResult.getErrorCode()).isEqualTo(0);
        return assertDoesNotThrow(
                () -> testHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        logbookCreationResult.getPayload()
                )
        );
    }

    @Test
    public void searchWithText() {
        var newLogBookResult = getTestLogbook();
        // create some data
        for (int idx = 0; idx < 100; idx++) {
            int finalIdx = idx;
            ApiResultResponse<String> newLogID =
                    assertDoesNotThrow(
                            () -> testHelperService.createNewLog(
                                    mockMvc,
                                    status().isCreated(),
                                    EntryNewDTO
                                            .builder()
                                            .logbook(newLogBookResult.getPayload().name())
                                            .text("This is a log for test")
                                            .segment(String.valueOf(finalIdx))
                                            .title("A very wonderful log for index=" + finalIdx)
                                            .build()
                            )
                    );
            AssertionsForClassTypes.assertThat(newLogID).isNotNull();
        }

        // initial search
        ApiResultResponse<List<EntrySummaryDTO>> findTags = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.of("index=0"),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        assertThat(findTags).isNotNull();
        assertThat(findTags.getPayload().size()).isNotEqualTo(0);
    }

    @Test
    public void createLogWithTagFailWithNoSave() {
        var newLogBookResult = getTestLogbook();
        ControllerLogicException exception =
                assertThrows(
                        ControllerLogicException.class,
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().is4xxClientError(),
                                        EntryNewDTO
                                                .builder()
                                                .logbook(newLogBookResult.getPayload().name())
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .tags(List.of("tag-1", "tag-2"))
                                                .build()
                                )
                );
        AssertionsForClassTypes.assertThat(exception.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void createLogWithTagOK() {
        ApiResultResponse<LogbookDTO> logbookCreationResult = getTestLogbook();
        ApiResultResponse<String> tag01Id =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLogbookTags(
                                        mockMvc,
                                        status().isCreated(),
                                        logbookCreationResult.getPayload().id(),
                                        NewTagDTO
                                                .builder()
                                                .name("tag-1")
                                                .build()
                                )
                );
        ApiResultResponse<String> tag02Id =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLogbookTags(
                                        mockMvc,
                                        status().isCreated(),
                                        logbookCreationResult.getPayload().id(),
                                        NewTagDTO
                                                .builder()
                                                .name("tag-2")
                                                .build()
                                )
                );
        ApiResultResponse<String> logID =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().isCreated(),
                                        EntryNewDTO
                                                .builder()
                                                .logbook(logbookCreationResult.getPayload().name())
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .tags(List.of("tag-1", "tag-2"))
                                                .build()
                                )
                );
        AssertionsForClassTypes.assertThat(logID.getErrorCode()).isEqualTo(0);
    }

    @Test
    public void getAllTags() {
        ApiResultResponse<LogbookDTO> logbookCreationResult = getTestLogbook();
        ApiResultResponse<String> tag01Id =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLogbookTags(
                                        mockMvc,
                                        status().isCreated(),
                                        logbookCreationResult.getPayload().id(),
                                        NewTagDTO
                                                .builder()
                                                .name("tag-1")
                                                .build()
                                )
                );
        ApiResultResponse<String> tag02Id =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLogbookTags(
                                        mockMvc,
                                        status().isCreated(),
                                        logbookCreationResult.getPayload().id(),
                                        NewTagDTO
                                                .builder()
                                                .name("tag-2")
                                                .build()
                                )
                );
        ApiResultResponse<List<TagDTO>> allTags =
                assertDoesNotThrow(
                        () ->
                                testHelperService.getLogbookTags(
                                        mockMvc,
                                        status().isOk(),
                                        logbookCreationResult.getPayload().id()
                                )
                );
        AssertionsForInterfaceTypes.assertThat(
                        allTags.getPayload()
                )
                .extracting("name")
                .containsAll(
                        List.of("tag-1", "tag-2")
                );
    }
}
