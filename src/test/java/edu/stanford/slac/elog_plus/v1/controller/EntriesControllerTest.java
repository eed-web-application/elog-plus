package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.EntryNotFound;
import edu.stanford.slac.elog_plus.exception.SupersedeAlreadyCreated;
import edu.stanford.slac.elog_plus.exception.TagNotFound;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.LogbookService;
import edu.stanford.slac.elog_plus.v1.service.DocumentGenerationService;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.AssertionsForInterfaceTypes;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
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

    @Autowired
    private DocumentGenerationService documentGenerationService;

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
                                                .logbooks(List.of(newLogBookResult.getPayload().id()))
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .build()
                                )
                );

        AssertionsForClassTypes.assertThat(newLogID).isNotNull();
        AssertionsForClassTypes.assertThat(newLogID.getErrorCode()).isEqualTo(0);
    }

    @Test
    public void createNewLogWithAttachement() throws Exception {
        ApiResultResponse<String> newAttachmentID = null;
        try (InputStream is = documentGenerationService.getTestPng()) {
            newAttachmentID = assertDoesNotThrow(
                    () -> testHelperService.newAttachment(
                            mockMvc,
                            status().isCreated(),
                            new MockMultipartFile(
                                    "uploadFile",
                                    "test.png",
                                    MediaType.IMAGE_PNG_VALUE,
                                    is
                            )
                    )
            );
        }
        assertThat(newAttachmentID.getErrorCode()).isEqualTo(0);

        var newLogBookResult = getTestLogbook();
        ApiResultResponse<String> finalNewAttachmentID = newAttachmentID;
        ApiResultResponse<String> newLogID =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().isCreated(),
                                        EntryNewDTO
                                                .builder()
                                                .logbooks(List.of(newLogBookResult.getPayload().id()))
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .attachments(
                                                        List.of(
                                                                finalNewAttachmentID.getPayload()
                                                        )
                                                )
                                                .build()
                                )
                );

        AssertionsForClassTypes.assertThat(newLogID).isNotNull();
        AssertionsForClassTypes.assertThat(newLogID.getErrorCode()).isEqualTo(0);
        await().atMost(30, SECONDS).pollDelay(2, MILLISECONDS).until(
                () -> {
                    ApiResultResponse<EntryDTO> logDto = assertDoesNotThrow(
                            () ->
                                    testHelperService.getFullLog(
                                            mockMvc,
                                            newLogID.getPayload()
                                    )
                    );
                    assertThat(logDto.getErrorCode()).isEqualTo(0);
                    return logDto.getPayload().attachments().size()==1 &&
                            logDto.getPayload().attachments().get(0).previewState().compareTo(Attachment.PreviewProcessingState.Completed.name()) ==0;
                }
        );

        ApiResultResponse<EntryDTO> logDto = assertDoesNotThrow(
                () ->
                        testHelperService.getFullLog(
                                mockMvc,
                                newLogID.getPayload()
                        )
        );
        assertThat(logDto.getErrorCode()).isEqualTo(0);
        assertThat(logDto.getPayload().attachments().size()).isEqualTo(1);
        assertThat(logDto.getPayload().attachments().get(0).miniPreview()).isNotNull();

    }

    @Test
    public void failCreatingNewLogWitWrongTag() throws Exception {
        var newLogBookResult = getTestLogbook();

        TagNotFound tagNotFound =
                assertThrows(
                        TagNotFound.class,
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().is4xxClientError(),
                                        EntryNewDTO
                                                .builder()
                                                .logbooks(List.of(newLogBookResult.getPayload().id()))
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .tags(
                                                        List.of(
                                                                "wrong id"
                                                        )
                                                )
                                                .build()
                                )
                );

        assertThat(tagNotFound.getErrorCode()).isEqualTo(-4);
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
                                                .logbooks(List.of(newLogBookResult.getPayload().id()))
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
                Optional.empty(),
                Optional.of(10),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
        AssertionsForClassTypes.assertThat(queryResult.getPayload().size()).isEqualTo(1);
    }

    @Test
    public void failsFetchingInvalidEntry() {
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
                                                .logbooks(List.of(newLogBookResult.getPayload().id()))
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
                                                .logbooks(List.of(newLogBookResult.getPayload().id()))
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
                                .logbooks(List.of(newLogBookResult.getPayload().id()))
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
                Optional.empty(),
                Optional.of(10),
                Optional.empty(),
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
                                                .logbooks(List.of(newLogBookResult.getPayload().id()))
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
                                .logbooks(List.of(newLogBookResult.getPayload().id()))
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
                                .logbooks(List.of(newLogBookResult.getPayload().id()))
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
                                .logbooks(List.of(newLogBookResult.getPayload().id()))
                                .text("This is a log for test")
                                .title("A very wonderful supersede log")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(exception.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void createNewSupersedeOfEntryWithFollowUp() throws Exception {
        var newLogBookResult = getTestLogbook();
        ApiResultResponse<String> newLogIDResult =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().isCreated(),
                                        EntryNewDTO
                                                .builder()
                                                .logbooks(List.of(newLogBookResult.getPayload().id()))
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .build()
                                )
                );

        AssertionsForClassTypes.assertThat(newLogIDResult).isNotNull();
        AssertionsForClassTypes.assertThat(newLogIDResult.getErrorCode()).isEqualTo(0);

        // create new followups
        ApiResultResponse<String> newFollowUpsLogIDResult = assertDoesNotThrow(
                () -> testHelperService.createNewFollowUpLog(
                        mockMvc,
                        status().isCreated(),
                        newLogIDResult.getPayload(),
                        EntryNewDTO
                                .builder()
                                .logbooks(List.of(newLogBookResult.getPayload().id()))
                                .text("This is a followup for entry %s".formatted(newLogIDResult.getPayload()))
                                .title("A very wonderful follow up log %s".formatted(newLogIDResult.getPayload()))
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(newFollowUpsLogIDResult).isNotNull();
        AssertionsForClassTypes.assertThat(newFollowUpsLogIDResult.getErrorCode()).isEqualTo(0);

        // create supersede of followed up logs
        ApiResultResponse<String> newSupersedeLogIDResult = assertDoesNotThrow(
                () -> testHelperService.createNewSupersedeLog(
                        mockMvc,
                        status().isCreated(),
                        newLogIDResult.getPayload(),
                        EntryNewDTO
                                .builder()
                                .logbooks(List.of(newLogBookResult.getPayload().id()))
                                .text("This is a log for test")
                                .title("A very wonderful supersede log")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(newSupersedeLogIDResult).isNotNull();
        AssertionsForClassTypes.assertThat(newSupersedeLogIDResult.getErrorCode()).isEqualTo(0);

        //check old log for supersede info
        ApiResultResponse<EntryDTO> supersededFull = assertDoesNotThrow(
                () ->
                        testHelperService.getFullLog(
                                mockMvc,
                                status().isOk(),
                                newLogIDResult.getPayload(),
                                true
                        )
        );

        assertThat(supersededFull.getErrorCode()).isEqualTo(0);
        assertThat(supersededFull.getPayload().supersedeBy()).isEqualTo(newSupersedeLogIDResult.getPayload());
        assertThat(supersededFull.getPayload().followUps()).isNotNull().isNotEmpty();

        //check new supersede log and check follow-ups
        ApiResultResponse<EntryDTO> supersedeFull = assertDoesNotThrow(
                () ->
                        testHelperService.getFullLog(
                                mockMvc,
                                status().isOk(),
                                newSupersedeLogIDResult.getPayload(),
                                true
                        )
        );

        assertThat(supersedeFull.getErrorCode()).isEqualTo(0);
        assertThat(supersedeFull.getPayload().supersedeBy()).isNull();

        assertThat(
                supersedeFull.getPayload().followUps()
        ).isEqualTo(
                supersededFull.getPayload().followUps()
        );
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
                                                .logbooks(List.of(newLogBookResult.getPayload().id()))
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
                                .logbooks(List.of(newLogBookResult.getPayload().id()))
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
                                .logbooks(List.of(newLogBookResult.getPayload().id()))
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
                                                .logbooks(List.of(newLogBookResult.getPayload().id()))
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
                                .logbooks(List.of(newLogBookResult.getPayload().id()))
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
                                .logbooks(List.of(newLogBookResult.getPayload().id()))
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
        assertThat(fullLog.getPayload().followUps()).isNull();

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
        assertThat(fullLogWitFollowUps.getPayload().followUps()).isNotNull();
        assertThat(fullLogWitFollowUps.getPayload().followUps().size()).isEqualTo(2);

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
                                            .logbooks(List.of(newLogBookResult.getPayload().id()))
                                            .text("This is a log for test")
                                            .title("A very wonderful log")
                                            .note(String.valueOf(finalIdx))
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
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        AssertionsForInterfaceTypes.assertThat(firstPageResult.getErrorCode()).isEqualTo(0);
        List<EntrySummaryDTO> firstPage = firstPageResult.getPayload();
        AssertionsForInterfaceTypes.assertThat(firstPage).isNotNull();
        AssertionsForClassTypes.assertThat(firstPage.size()).isEqualTo(10);
        String note = null;
        for (EntrySummaryDTO l :
                firstPage) {
            if (note == null) {
                note = l.note();
                continue;
            }
            AssertionsForClassTypes.assertThat(Integer.valueOf(note)).isGreaterThan(
                    Integer.valueOf(l.note())
            );
            note = l.note();
        }
        var testAnchorLog = firstPage.get(firstPage.size() - 1);
        // load next page
        ApiResultResponse<List<EntrySummaryDTO>> nextPageResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.of(testAnchorLog.id()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
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

        note = nextPage.get(0).note();
        nextPage.remove(0);
        for (EntrySummaryDTO l :
                nextPage) {
            AssertionsForClassTypes.assertThat(Integer.valueOf(note)).isGreaterThan(
                    Integer.valueOf(l.note())
            );
            note = l.note();
        }

        // now get all the record upside and downside
        ApiResultResponse<List<EntrySummaryDTO>> prevPageByPinResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.of(testAnchorLog.id()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
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
                        Optional.of(testAnchorLog.id()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.of(10),
                        Optional.empty(),
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
        // create new logbooks
        // create some data
        String[] tagIds = new String[100];
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
                                    .name(String.valueOf("tags-" + finalIdx))
                                    .build()
                    ));
            assertThat(newTagID).isNotNull();
            assertThat(newTagID.getErrorCode()).isEqualTo(0);
            tagIds[idx] = newTagID.getPayload();
            ApiResultResponse<String> newLogID =
                    assertDoesNotThrow(
                            () -> testHelperService.createNewLog(
                                    mockMvc,
                                    status().isCreated(),
                                    EntryNewDTO
                                            .builder()
                                            .logbooks(List.of(logbookCreationResult.getPayload().id()))
                                            .text("This is a log for test")
                                            .title("A very wonderful log")
                                            .note(String.valueOf(finalIdx))
                                            .tags(List.of(tagIds[finalIdx]))
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
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.of(List.of(tagIds[99], tagIds[49], tagIds[0])),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        assertThat(findTags).isNotNull();
        assertThat(findTags.getPayload().size()).isEqualTo(3);
        assertThat(findTags.getPayload().get(0).tags()).extracting("name").contains("tags-99");
        assertThat(findTags.getPayload().get(1).tags()).extracting("name").contains("tags-49");
        assertThat(findTags.getPayload().get(2).tags()).extracting("name").contains("tags-0");
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
                                            .logbooks(List.of(newLogBookResult.getPayload().id()))
                                            .text("This is a log for test")
                                            .note(String.valueOf(finalIdx))
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
                        Optional.empty(),
                        Optional.of(10),
                        Optional.of("index=0"),
                        Optional.empty(),
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
        TagNotFound tagNotFound =
                assertThrows(
                        TagNotFound.class,
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().is4xxClientError(),
                                        EntryNewDTO
                                                .builder()
                                                .logbooks(List.of(newLogBookResult.getPayload().id()))
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .tags(List.of("tag-1", "tag-2"))
                                                .build()
                                )
                );
        AssertionsForClassTypes.assertThat(tagNotFound.getErrorCode()).isEqualTo(-4);
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
                                                .logbooks(List.of(logbookCreationResult.getPayload().id()))
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .tags(List.of(tag01Id.getPayload(), tag02Id.getPayload()))
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

    @Test
    public void searchWithAnchorUsingLoggedAtInsteadEventAt() {
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
                                            .logbooks(List.of(newLogBookResult.getPayload().id()))
                                            .text("This is a log for test")
                                            .title("A very wonderful log")
                                            .note(String.valueOf(finalIdx))
                                            .eventAt(
                                                    LocalDateTime.now()
                                                            .minusDays(finalIdx)
                                            )
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
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(true)
                )
        );
        AssertionsForInterfaceTypes.assertThat(firstPageResult.getErrorCode()).isEqualTo(0);
        List<EntrySummaryDTO> firstPage = firstPageResult.getPayload();
        AssertionsForInterfaceTypes.assertThat(firstPage).isNotNull();
        AssertionsForClassTypes.assertThat(firstPage.size()).isEqualTo(10);
        String note = null;
        for (EntrySummaryDTO l :
                firstPage) {
            if (note == null) {
                note = l.note();
                continue;
            }
            AssertionsForClassTypes.assertThat(Integer.valueOf(note)).isGreaterThan(
                    Integer.valueOf(l.note())
            );
            note = l.note();
        }
        var testAnchorLog = firstPage.get(firstPage.size() - 1);
        // load next page
        ApiResultResponse<List<EntrySummaryDTO>> nextPageResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.of(testAnchorLog.id()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(true)
                )
        );
        AssertionsForInterfaceTypes.assertThat(nextPageResult.getErrorCode()).isEqualTo(0);
        List<EntrySummaryDTO> nextPage = nextPageResult.getPayload();
        AssertionsForInterfaceTypes.assertThat(nextPage).isNotNull();
        AssertionsForClassTypes.assertThat(nextPage.size()).isEqualTo(10);
        // check that the first of next page is not the last of the previous
        AssertionsForClassTypes.assertThat(nextPage.get(0).id()).isNotEqualTo(firstPage.get(firstPage.size() - 1).id());

        note = nextPage.get(0).note();
        nextPage.remove(0);
        for (EntrySummaryDTO l :
                nextPage) {
            AssertionsForClassTypes.assertThat(Integer.valueOf(note)).isGreaterThan(
                    Integer.valueOf(l.note())
            );
            note = l.note();
        }

        // now get all the record upside and downside
        ApiResultResponse<List<EntrySummaryDTO>> prevPageByPinResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.of(testAnchorLog.id()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(true)
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
                        Optional.of(testAnchorLog.id()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(true)
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
    public void searchWithAnchorUsingReversedEventAtAndDefaultOrder() {
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
                                            .logbooks(List.of(newLogBookResult.getPayload().id()))
                                            .text("This is a log for test")
                                            .title("A very wonderful log")
                                            .note(String.valueOf(finalIdx))
                                            .eventAt(
                                                    LocalDateTime.now()
                                                            .minusDays(finalIdx)
                                            )
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
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        AssertionsForInterfaceTypes.assertThat(firstPageResult.getErrorCode()).isEqualTo(0);
        List<EntrySummaryDTO> firstPage = firstPageResult.getPayload();
        AssertionsForInterfaceTypes.assertThat(firstPage).isNotNull();
        AssertionsForClassTypes.assertThat(firstPage.size()).isEqualTo(10);
        String note = null;
        for (EntrySummaryDTO l :
                firstPage) {
            if (note == null) {
                note = l.note();
                continue;
            }
            AssertionsForClassTypes.assertThat(Integer.valueOf(note)).isLessThan(
                    Integer.valueOf(l.note())
            );
            note = l.note();
        }
        var testAnchorLog = firstPage.get(firstPage.size() - 1);
        // load next page
        ApiResultResponse<List<EntrySummaryDTO>> nextPageResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.of(testAnchorLog.id()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
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

        note = nextPage.get(0).note();
        nextPage.remove(0);
        for (EntrySummaryDTO l :
                nextPage) {
            AssertionsForClassTypes.assertThat(Integer.valueOf(note)).isLessThan(
                    Integer.valueOf(l.note())
            );
            note = l.note();
        }

        // now get all the record upside and downside
        ApiResultResponse<List<EntrySummaryDTO>> prevPageByPinResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.of(testAnchorLog.id()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
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
                        Optional.of(testAnchorLog.id()),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.of(10),
                        Optional.empty(),
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
    public void searchWithAnchorUsingReversedEventAtAndDefaultOrderWithDateLimit() {
        LocalDateTime now = LocalDateTime.now();
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
                                            .logbooks(List.of(newLogBookResult.getPayload().id()))
                                            .text("This is a log for test")
                                            .title("A very wonderful log")
                                            .note(String.valueOf(finalIdx))
                                            .eventAt(
                                                    now.minusDays(finalIdx)
                                            )
                                            .build()
                            )
                    );
            AssertionsForClassTypes.assertThat(newLogID).isNotNull();
        }

        // initial with past limit
        ApiResultResponse<List<EntrySummaryDTO>> firstPageResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.empty(),
                        Optional.of(now.minusDays(10)),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(20),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        AssertionsForInterfaceTypes.assertThat(firstPageResult.getErrorCode()).isEqualTo(0);
        List<EntrySummaryDTO> firstPage = firstPageResult.getPayload();
        AssertionsForInterfaceTypes.assertThat(firstPage).isNotNull();
        AssertionsForClassTypes.assertThat(firstPage.size()).isEqualTo(11);
        AssertionsForClassTypes.assertThat(firstPage.get(0).eventAt().getDayOfMonth()).isEqualTo(now.getDayOfMonth());
        AssertionsForClassTypes.assertThat(firstPage.get(10).eventAt().getDayOfMonth()).isEqualTo(now.minusDays(10).getDayOfMonth());


        // end and start limit
        ApiResultResponse<List<EntrySummaryDTO>> secondPageResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.empty(),
                        Optional.of(now.minusDays(10)),
                        Optional.of(now.minusDays(5)),
                        Optional.empty(),
                        Optional.of(20),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        AssertionsForInterfaceTypes.assertThat(secondPageResult.getErrorCode()).isEqualTo(0);
        List<EntrySummaryDTO> secondPage = secondPageResult.getPayload();
        AssertionsForInterfaceTypes.assertThat(secondPage).isNotNull();
        AssertionsForClassTypes.assertThat(secondPage.size()).isEqualTo(6);
        AssertionsForClassTypes.assertThat(secondPage.get(0).eventAt().getDayOfMonth()).isEqualTo(now.minusDays(5).getDayOfMonth());
        AssertionsForClassTypes.assertThat(secondPage.get(5).eventAt().getDayOfMonth()).isEqualTo(now.minusDays(10).getDayOfMonth());
    }

    @Test
    public void searchWithAnchorUsingReversedEventAtAndDefaultOrderWithDateLimitAndPaging() {
        LocalDateTime now = LocalDateTime.now();
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
                                            .logbooks(List.of(newLogBookResult.getPayload().id()))
                                            .text("This is a log for test")
                                            .title("A very wonderful log")
                                            .note(String.valueOf(finalIdx))
                                            .eventAt(
                                                    now.minusDays(finalIdx)
                                            )
                                            .build()
                            )
                    );
            AssertionsForClassTypes.assertThat(newLogID).isNotNull();
        }

        boolean getNextPage = true;
        EntrySummaryDTO anchorEntry = null;
        EntrySummaryDTO firstEntryFirstPage = null;
        EntrySummaryDTO lastEntryLastPage = null;
        while (getNextPage) {
            // initial with past limit
            EntrySummaryDTO finalAnchorEntry = anchorEntry;
            ApiResultResponse<List<EntrySummaryDTO>> nextPage = assertDoesNotThrow(
                    () -> testHelperService.submitSearchByGetWithAnchor(
                            mockMvc,
                            status().isOk(),
                            (finalAnchorEntry == null) ? Optional.empty() : Optional.of(finalAnchorEntry.id()),
                            Optional.of(now.minusDays(50)),
                            Optional.of(now.minusDays(1)),
                            Optional.empty(),
                            Optional.of(10),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty(),
                            Optional.empty()
                    )
            );
            AssertionsForInterfaceTypes.assertThat(nextPage.getErrorCode()).isEqualTo(0);
            if (firstEntryFirstPage == null) {
                firstEntryFirstPage = nextPage.getPayload().get(0);
            } else if (!nextPage.getPayload().isEmpty()) {
                lastEntryLastPage = nextPage.getPayload().get(nextPage.getPayload().size() - 1);
            }

            if (nextPage.getPayload().size() < 10) {
                getNextPage = false;
            } else {
                anchorEntry = nextPage.getPayload().get(9);
            }
        }
        assertThat(firstEntryFirstPage.eventAt().getDayOfMonth()).isEqualTo(now.minusDays(1).getDayOfMonth());
        assertThat(lastEntryLastPage.eventAt().getDayOfMonth()).isEqualTo(now.minusDays(50).getDayOfMonth());
    }

    @Test
    public void findSummaryId() {
        var newLogBookResult = getTestLogbook();
        ApiResultResponse<LogbookDTO> finalNewLogBookResult2 = newLogBookResult;
        ApiResultResponse<Boolean> replacementResult = assertDoesNotThrow(
                () -> testHelperService.updateLogbook(
                        mockMvc,
                        status().isCreated(),
                        finalNewLogBookResult2.getPayload().id(),
                        UpdateLogbookDTO
                                .builder()
                                .name(finalNewLogBookResult2.getPayload().name())
                                .tags(
                                        Collections.emptyList()
                                )
                                .shifts(
                                        List.of(
                                                ShiftDTO.builder()
                                                        .id(null)
                                                        .name("Morning Shift")
                                                        .from("16:09")
                                                        .to("17:09")
                                                        .build()
                                        )
                                )
                                .build()
                )
        );

        ApiResultResponse<LogbookDTO> finalNewLogBookResult = newLogBookResult;
        newLogBookResult = assertDoesNotThrow(
                () -> testHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        finalNewLogBookResult.getPayload().id()
                )
        );

        ApiResultResponse<LogbookDTO> finalNewLogBookResult1 = newLogBookResult;
        ApiResultResponse<String> newLogID =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        status().isCreated(),
                                        EntryNewDTO
                                                .builder()
                                                .logbooks(List.of(finalNewLogBookResult1.getPayload().id()))
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .summarizes(
                                                        SummarizesDTO
                                                                .builder()
                                                                .shiftId(finalNewLogBookResult1.getPayload().shifts().get(0).id())
                                                                .date(LocalDate.now())
                                                                .build()
                                                )
                                                .build()
                                )
                );

        AssertionsForClassTypes.assertThat(newLogID).isNotNull();
        AssertionsForClassTypes.assertThat(newLogID.getErrorCode()).isEqualTo(0);

        ApiResultResponse<String> foundSummaryID = assertDoesNotThrow(
                () ->
                        testHelperService.findSummaryIdByShiftNameAndDate(
                                mockMvc,
                                status().isOk(),
                                finalNewLogBookResult1.getPayload().shifts().get(0).id(),
                                LocalDate.now()
                        )
        );
        AssertionsForClassTypes.assertThat(foundSummaryID).isNotNull();
        AssertionsForClassTypes.assertThat(foundSummaryID.getErrorCode()).isEqualTo(0);
        AssertionsForClassTypes.assertThat(foundSummaryID.getPayload()).isEqualTo(newLogID.getPayload());
    }
}
