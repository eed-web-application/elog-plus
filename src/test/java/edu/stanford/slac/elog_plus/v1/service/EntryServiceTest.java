package edu.stanford.slac.elog_plus.v1.service;

import com.github.javafaker.Faker;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.exception.EntryNotFound;
import edu.stanford.slac.elog_plus.exception.ShiftNotFound;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import edu.stanford.slac.elog_plus.service.EntryService;
import edu.stanford.slac.elog_plus.service.LogbookService;
import edu.stanford.slac.elog_plus.utility.DateUtilities;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class EntryServiceTest {
    @Autowired
    private EntryService entryService;
    @Autowired
    private LogbookService logbookService;
    @Autowired
    private AttachmentService attachmentService;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Entry.class);
        mongoTemplate.remove(new Query(), Logbook.class);
    }

    private LogbookDTO getTestLogbook() {
        String logbookId =
                assertDoesNotThrow(
                        () -> logbookService.createNew(
                                NewLogbookDTO
                                        .builder()
                                        .name(UUID.randomUUID().toString())
                                        .build()
                        )
                );
        return assertDoesNotThrow(
                () -> logbookService.getLogbook(logbookId)
        );
    }

    @Test
    public void testLogCreation() {
        var logbook = getTestLogbook();
        String newLogID = entryService.createNew(
                EntryNewDTO
                        .builder()
                        .logbooks(List.of(logbook.id()))
                        .text("This is a log for test")
                        .title("A very wonderful log")
                        .build()
        );

        assertThat(newLogID).isNotNull();
    }

    @Test
    public void testLogTextSanitization() {
        var logbook = getTestLogbook();
        String newLogID = entryService.createNew(
                EntryNewDTO
                        .builder()
                        .logbooks(List.of(logbook.id()))
                        .text("<h1>H1</h1><h2>H2</h2><p><a href='http://example.com/' onclick='stealCookies()'>Link</a></p>")
                        .title("A very wonderful log")
                        .build()
        );

        EntryDTO fullLog =
                assertDoesNotThrow(
                        () -> entryService.getFullEntry(
                                newLogID
                        )
                );
        assertThat(fullLog.text()).isEqualTo("<h1>H1</h1>\n<h2>H2</h2>\n<p><a href=\"http://example.com/\" rel=\"nofollow\">Link</a></p>");
    }

    @Test
    public void testFailBadAttachmentID() {
        var logbook = getTestLogbook();
        ControllerLogicException ex =
                assertThrows(
                        ControllerLogicException.class,
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .attachments(List.of("wrong id"))
                                        .build()
                        )
                );
        assertThat(ex.getErrorCode()).isEqualTo(-3);
    }

    @Test
    public void failGettingNotFoundLog() {
        EntryNotFound exception =
                assertThrows(
                        EntryNotFound.class,
                        () -> entryService.getFullEntry("wrong id")
                );
        assertThat(exception.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void testFetchFullLog() {
        var logbook = getTestLogbook();
        String newLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );

        EntryDTO fullLog =
                assertDoesNotThrow(
                        () -> entryService.getFullEntry(newLogID)
                );
        assertThat(fullLog.id()).isEqualTo(newLogID);
    }

    @Test
    public void testEventAtFetchFullLog() {
        var logbook = getTestLogbook();
        var eventAt = LocalDateTime.of(
                2023,
                7,
                1,
                0,
                1,
                0
        );
        String newLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .eventAt(
                                                eventAt
                                        )
                                        .build()
                        )
                );

        EntryDTO fullLog =
                assertDoesNotThrow(
                        () -> entryService.getFullEntry(newLogID)
                );
        assertThat(fullLog.id()).isEqualTo(newLogID);
        assertThat(fullLog.eventAt()).isEqualTo(eventAt);
    }

    @Test
    public void testWithoutEventAtFetchFullLog() {
        var logbook = getTestLogbook();
        String newLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );

        EntryDTO fullLog =
                assertDoesNotThrow(
                        () -> entryService.getFullEntry(newLogID)
                );
        assertThat(fullLog.id()).isEqualTo(newLogID);
        assertThat(fullLog.eventAt()).isEqualTo(fullLog.loggedAt());
    }

    @Test
    public void testSupersedeCreationFailOnWrongRootLog() {
        var logbook = getTestLogbook();
        ControllerLogicException exception = assertThrows(
                ControllerLogicException.class,
                () -> entryService.createNewSupersede(
                        "bad id",
                        EntryNewDTO
                                .builder()
                                .logbooks(List.of(logbook.id()))
                                .text("This is a log for test")
                                .title("A very wonderful log")
                                .build()
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void testSupersedeOK() {
        var logbook = getTestLogbook();
        String supersededLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );

        String newLogID =
                assertDoesNotThrow(
                        () -> entryService.createNewSupersede(
                                supersededLogID,
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for supersede")
                                        .build()
                        )
                );

        EntryDTO supersededLog = assertDoesNotThrow(
                () -> entryService.getFullEntry(supersededLogID)
        );

        assertThat(supersededLog).isNotNull();
        assertThat(supersededLog.supersedeBy()).isEqualTo(newLogID);

        EntryDTO fullLog = assertDoesNotThrow(
                () -> entryService.getFullEntry(newLogID)
        );

        assertThat(fullLog).isNotNull();
        assertThat(fullLog.id()).isEqualTo(newLogID);
    }

    @Test
    public void testHistory() {
        var logbook = getTestLogbook();
        String supersededLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );

        String supersededLogIDTwo =
                assertDoesNotThrow(
                        () -> entryService.createNewSupersede(
                                supersededLogID,
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for supersede")
                                        .build()
                        )
                );

        String supersededLogIDNewest =
                assertDoesNotThrow(
                        () -> entryService.createNewSupersede(
                                supersededLogIDTwo,
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for supersede of supersede")
                                        .build()
                        )
                );
        List<EntryDTO> history = new ArrayList<>();
        assertDoesNotThrow(
                () -> entryService.getLogHistory(supersededLogIDNewest, history)
        );
        assertThat(history).isNotEmpty();
        assertThat(history).extracting("id").containsExactly(supersededLogIDTwo, supersededLogID);
    }

    @Test
    public void testSupersedeErrorOnDoubleSuperseding() {
        var logbook = getTestLogbook();
        String supersededLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );
        assertThat(supersededLogID).isNotNull();

        String newLogID =
                assertDoesNotThrow(
                        () -> entryService.createNewSupersede(
                                supersededLogID,
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for supersede")
                                        .build()
                        )
                );
        assertThat(newLogID).isNotNull();

        ControllerLogicException exception =
                assertThrows(
                        ControllerLogicException.class,
                        () -> entryService.createNewSupersede(
                                supersededLogID,
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for supersede one more time")
                                        .build()
                        )
                );
        assertThat(exception.getErrorCode()).isEqualTo(-3);
    }

    @Test
    public void testFollowUpCreationFailOnWrongRootLog() {
        var logbook = getTestLogbook();
        ControllerLogicException exception = assertThrows(
                ControllerLogicException.class,
                () -> entryService.createNewFollowUp(
                        "bad root id",
                        EntryNewDTO
                                .builder()
                                .logbooks(List.of(logbook.id()))
                                .text("This is a log for test")
                                .title("A very wonderful log")
                                .build()
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void testFollowUpCreation() {
        var logbook = getTestLogbook();
        String rootLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );
        assertThat(rootLogID).isNotNull();

        String newFollowUpOneID =
                assertDoesNotThrow(
                        () -> entryService.createNewFollowUp(
                                rootLogID,
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for followUps one")
                                        .build()
                        )
                );
        assertThat(newFollowUpOneID).isNotNull();

        String newFollowUpTwoID =
                assertDoesNotThrow(
                        () -> entryService.createNewFollowUp(
                                rootLogID,
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for followUps two")
                                        .build()
                        )
                );
        assertThat(newFollowUpTwoID).isNotNull();

        List<EntrySummaryDTO> followUpLogsFound =
                assertDoesNotThrow(
                        () -> entryService.getAllFollowUpForALog(
                                rootLogID
                        )
                );

        assertThat(followUpLogsFound).isNotNull();
        assertThat(followUpLogsFound.size()).isEqualTo(2);
    }

    @Test
    public void testFollowingUpIngFullLog() {
        var logbook = getTestLogbook();
        String rootLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );
        assertThat(rootLogID).isNotNull();

        String newFollowUpOneID =
                assertDoesNotThrow(
                        () -> entryService.createNewFollowUp(
                                rootLogID,
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for followUps one")
                                        .build()
                        )
                );
        assertThat(newFollowUpOneID).isNotNull();

        EntryDTO logWithFlowingUpLog =
                assertDoesNotThrow(
                        () -> entryService.getFullEntry(
                                newFollowUpOneID,
                                Optional.empty(),
                                Optional.of(true),
                                Optional.empty(),
                                Optional.of(true),
                                Optional.of(true)
                        )
                );

        assertThat(logWithFlowingUpLog).isNotNull();
        assertThat(logWithFlowingUpLog.followingUp()).isNotNull();
        assertThat(logWithFlowingUpLog.followingUp().id()).isEqualTo(rootLogID);
    }

    @Test
    public void testLogAttachmentOnFullDTO() {
        var logbook = getTestLogbook();
        Faker f = new Faker();
        String fileName = f.file().fileName(
                null,
                null,
                "pdf",
                null
        );
        String attachmentID =
                assertDoesNotThrow(
                        () -> attachmentService.createAttachment(
                                FileObjectDescription
                                        .builder()
                                        .fileName(fileName)
                                        .contentType(MediaType.APPLICATION_PDF_VALUE)
                                        .is(
                                                new ByteArrayInputStream(
                                                        "<<pdf data>>".getBytes(StandardCharsets.UTF_8)
                                                )
                                        )
                                        .build(),
                                false
                        )
                );
        assertThat(attachmentID).isNotNull();
        String newLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .attachments(List.of(attachmentID))
                                        .build()
                        )
                );

        // check for the attachment are well filled into dto
        EntryDTO foundLog =
                assertDoesNotThrow(
                        () -> entryService.getFullEntry(newLogID)
                );
        assertThat(foundLog).isNotNull();
        assertThat(foundLog.attachments().size()).isEqualTo(1);
        assertThat(foundLog.attachments().get(0).fileName()).isEqualTo(fileName);
        assertThat(foundLog.attachments().get(0).contentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
    }

    @Test
    public void testLogAttachmentOnSearchDTO() {
        var logbook = getTestLogbook();
        Faker f = new Faker();
        String fileName = f.file().fileName(
                null,
                null,
                "pdf",
                null
        );
        String attachmentID =
                assertDoesNotThrow(
                        () -> attachmentService.createAttachment(
                                FileObjectDescription
                                        .builder()
                                        .fileName(fileName)
                                        .contentType(MediaType.APPLICATION_PDF_VALUE)
                                        .is(
                                                new ByteArrayInputStream(
                                                        "<<pdf data>>".getBytes(StandardCharsets.UTF_8)
                                                )
                                        )
                                        .build(),
                                false
                        )
                );
        assertThat(attachmentID).isNotNull();
        String newLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbooks(List.of(logbook.id()))
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .attachments(List.of(attachmentID))
                                        .build()
                        )
                );
        assertThat(newLogID).isNotNull();
        // check for the attachment are well filled into dto
        var foundLog =
                assertDoesNotThrow(
                        () -> entryService.searchAll(
                                QueryWithAnchorDTO
                                        .builder()
                                        .limit(10)
                                        .build()
                        )
                );
        assertThat(foundLog).isNotNull();
        assertThat(foundLog.size()).isEqualTo(1);
        assertThat(foundLog.get(0).attachments().size()).isEqualTo(1);
        assertThat(foundLog.get(0).attachments().get(0).fileName()).isEqualTo(fileName);
        assertThat(foundLog.get(0).attachments().get(0).contentType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
    }

    @Test
    public void searchLogsByAnchor() {
        var logbook = getTestLogbook();
        String anchorID = null;
        // create some data
        for (int idx = 0; idx < 100; idx++) {
            int finalIdx = idx;
            String newLogID =
                    assertDoesNotThrow(
                            () -> entryService.createNew(
                                    EntryNewDTO
                                            .builder()
                                            .logbooks(List.of(logbook.id()))
                                            .text("This is a log for test")
                                            .title("A very wonderful log")
                                            .note(String.valueOf(finalIdx))
                                            .build()
                            )
                    );
            assertThat(newLogID).isNotNull();
            if (idx == 49) {
                anchorID = newLogID;
            }
        }

        // initial search
        List<EntrySummaryDTO> firstPage = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .limit(10)
                                .build()
                )
        );
        assertThat(firstPage).isNotNull();
        assertThat(firstPage.size()).isEqualTo(10);
        String note = null;
        for (EntrySummaryDTO l :
                firstPage) {
            if (note == null) {
                note = l.note();
                continue;
            }
            assertThat(Integer.valueOf(note)).isGreaterThan(
                    Integer.valueOf(l.note())
            );
            note = l.note();
        }
        var testAnchorLog = firstPage.get(firstPage.size() - 1);
        // load next page
        List<EntrySummaryDTO> nextPage = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .anchorID(testAnchorLog.id())
                                .limit(10)
                                .build()
                )
        );

        assertThat(nextPage).isNotNull();
        assertThat(nextPage.size()).isEqualTo(10);
        // check that the first of next page is not the last of the previous
        assertThat(nextPage.get(0).id()).isNotEqualTo(firstPage.get(firstPage.size() - 1).id());

        note = nextPage.get(0).note();
        nextPage.remove(0);
        for (EntrySummaryDTO l :
                nextPage) {
            assertThat(Integer.valueOf(note)).isGreaterThan(
                    Integer.valueOf(l.note())
            );
            note = l.note();
        }

        // now get all the record upside and downside
        List<EntrySummaryDTO> prevPageByPin = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .anchorID(testAnchorLog.id())
                                .contextSize(10)
                                .limit(0)
                                .build()
                )
        );
        assertThat(prevPageByPin).isNotNull();
        assertThat(prevPageByPin.size()).isEqualTo(10);
        assertThat(prevPageByPin).isEqualTo(firstPage);

        List<EntrySummaryDTO> prevAndNextPageByPin = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .anchorID(testAnchorLog.id())
                                .contextSize(10)
                                .limit(10)
                                .build()
                )
        );

        assertThat(prevAndNextPageByPin).isNotNull();
        assertThat(prevAndNextPageByPin.size()).isEqualTo(20);
        assertThat(prevAndNextPageByPin).containsAll(firstPage);
        assertThat(prevAndNextPageByPin).containsAll(nextPage);


        // now search in the middle of the data set
        EntryDTO middleAnchorLog = entryService.getFullEntry(anchorID);
        List<EntrySummaryDTO> prevAndNextPageByMiddlePin = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .anchorID(middleAnchorLog.id())
                                .contextSize(10)
                                .limit(10)
                                .build()
                )
        );
        assertThat(prevAndNextPageByMiddlePin).isNotNull();
        assertThat(prevAndNextPageByMiddlePin.size()).isEqualTo(20);
        assertThat(prevAndNextPageByMiddlePin.get(0).note()).isEqualTo("58");
        assertThat(prevAndNextPageByMiddlePin.get(19).note()).isEqualTo("39");
    }

    @Test
    public void searchLogsByAnchorReverseEventAtAndOrderedByLogged() {
        LocalDateTime now = LocalDateTime.now();
        var logbook = getTestLogbook();
        String anchorID = null;
        // create some data
        for (int idx = 0; idx < 100; idx++) {
            int finalIdx = idx;
            String newLogID =
                    assertDoesNotThrow(
                            () -> entryService.createNew(
                                    EntryNewDTO
                                            .builder()
                                            .logbooks(List.of(logbook.id()))
                                            .text("This is a log for test")
                                            .title("A very wonderful log")
                                            .note(String.valueOf(finalIdx))
                                            .eventAt(
                                                    now.minusDays(finalIdx)
                                            )
                                            .build()
                            )
                    );
            assertThat(newLogID).isNotNull();
            if (idx == 49) {
                anchorID = newLogID;
            }
        }

        // initial search
        List<EntrySummaryDTO> firstPage = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .limit(10)
                                .sortByLogDate(true)
                                .build()
                )
        );
        assertThat(firstPage).isNotNull();
        assertThat(firstPage.size()).isEqualTo(10);
        String note = null;
        for (EntrySummaryDTO l :
                firstPage) {
            if (note == null) {
                note = l.note();
                continue;
            }
            assertThat(Integer.valueOf(note)).isGreaterThan(
                    Integer.valueOf(l.note())
            );
            note = l.note();
        }
        var testAnchorLog = firstPage.get(firstPage.size() - 1);
        // load next page
        List<EntrySummaryDTO> nextPage = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .anchorID(testAnchorLog.id())
                                .limit(10)
                                .sortByLogDate(true)
                                .build()
                )
        );

        assertThat(nextPage).isNotNull();
        assertThat(nextPage.size()).isEqualTo(10);
        // check that the first of next page is not the last of the previous
        assertThat(nextPage.get(0).id()).isNotEqualTo(firstPage.get(firstPage.size() - 1).id());

        note = nextPage.get(0).note();
        nextPage.remove(0);
        for (EntrySummaryDTO l :
                nextPage) {
            assertThat(Integer.valueOf(note)).isGreaterThan(
                    Integer.valueOf(l.note())
            );
            note = l.note();
        }

        // now get all the record upside and downside
        List<EntrySummaryDTO> prevPageByPin = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .anchorID(testAnchorLog.id())
                                .contextSize(10)
                                .limit(0)
                                .sortByLogDate(true)
                                .build()
                )
        );
        assertThat(prevPageByPin).isNotNull();
        assertThat(prevPageByPin.size()).isEqualTo(10);
        assertThat(prevPageByPin).isEqualTo(firstPage);

        List<EntrySummaryDTO> prevAndNextPageByPin = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .anchorID(testAnchorLog.id())
                                .contextSize(10)
                                .limit(10)
                                .sortByLogDate(true)
                                .build()
                )
        );

        assertThat(prevAndNextPageByPin).isNotNull();
        assertThat(prevAndNextPageByPin.size()).isEqualTo(20);
        assertThat(prevAndNextPageByPin).containsAll(firstPage);
        assertThat(prevAndNextPageByPin).containsAll(nextPage);


        // now search in the middle of the data set
        EntryDTO middleAnchorLog = entryService.getFullEntry(anchorID);
        List<EntrySummaryDTO> prevAndNextPageByMiddlePin = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .anchorID(middleAnchorLog.id())
                                .contextSize(10)
                                .limit(10)
                                .sortByLogDate(true)
                                .build()
                )
        );
        assertThat(prevAndNextPageByMiddlePin).isNotNull();
        assertThat(prevAndNextPageByMiddlePin.size()).isEqualTo(20);
        assertThat(prevAndNextPageByMiddlePin.get(0).note()).isEqualTo("58");
        assertThat(prevAndNextPageByMiddlePin.get(19).note()).isEqualTo("39");
    }

    @Test
    public void searchLogResultShowCorrectShift() {
        var logbook = getTestLogbook();

        //add shifts
        logbookService.replaceShift(
                logbook.id(),
                List.of(
                        ShiftDTO
                                .builder()
                                .name("Shift1")
                                .from(
                                        DateUtilities.toUTCString(
                                                LocalTime.of(
                                                        0,
                                                        0
                                                )
                                        )
                                )
                                .to(
                                        DateUtilities.toUTCString(
                                                LocalTime.of(
                                                        7,
                                                        59
                                                )
                                        )
                                )
                                .build(),
                        ShiftDTO
                                .builder()
                                .name("Shift2")
                                .from(
                                        DateUtilities.toUTCString(
                                                LocalTime.of(
                                                        8,
                                                        0
                                                )
                                        )
                                )
                                .to(
                                        DateUtilities.toUTCString(
                                                LocalTime.of(
                                                        12,
                                                        59
                                                )
                                        )
                                )
                                .build()
                )
        );

        String anchorID = null;
        // create some data
        Random random = new Random();
        for (int idx = 0; idx < 30; idx++) {
            String newLogID =
                    assertDoesNotThrow(
                            () -> entryService.createNew(
                                    EntryNewDTO
                                            .builder()
                                            .logbooks(List.of(logbook.id()))
                                            .text("This is a log for test")
                                            .title("A very wonderful log")
                                            .eventAt(
                                                    LocalDateTime.now().withHour(
                                                                    random.nextInt(24)
                                                            )
                                                            .withMinute(
                                                                    random.nextInt(60)
                                                            )
                                            )
                                            .build()
                            )
                    );
            assertThat(newLogID).isNotNull();
        }

        // initial search
        List<EntrySummaryDTO> firstPage = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .limit(30)
                                .build()
                )
        );
        assertThat(firstPage).isNotNull();
        assertThat(firstPage.size()).isEqualTo(30);

        Condition<EntrySummaryDTO> outOfShift = new Condition<>(
                e -> e.eventAt().toLocalTime().equals(
                        LocalTime.of(
                                13,
                                0
                        )
                ) || e.eventAt().toLocalTime().equals(
                        LocalTime.of(
                                23,
                                59
                        )
                ) || e.eventAt().toLocalTime().isAfter(
                        LocalTime.of(
                                13,
                                0
                        )
                ) || e.eventAt().toLocalTime().isBefore(
                        LocalTime.of(
                                23,
                                59
                        )
                )
                ,
                "no shift");
        Condition<EntrySummaryDTO> shift1 = new Condition<>(
                e -> e.eventAt().toLocalTime().equals(
                        LocalTime.of(
                                0,
                                0
                        )
                ) || e.eventAt().toLocalTime().equals(
                        LocalTime.of(
                                7,
                                59
                        )
                ) || e.eventAt().toLocalTime().isAfter(
                        LocalTime.of(
                                0,
                                0
                        )
                ) || e.eventAt().toLocalTime().isBefore(
                        LocalTime.of(
                                7,
                                59
                        )
                )
                ,
                "Shift1");
        Condition<EntrySummaryDTO> shift2 = new Condition<>(
                e -> e.eventAt().toLocalTime().equals(
                        LocalTime.of(
                                8,
                                0
                        )
                ) || e.eventAt().toLocalTime().equals(
                        LocalTime.of(
                                12,
                                59
                        )
                ) || e.eventAt().toLocalTime().isAfter(
                        LocalTime.of(
                                8,
                                0
                        )
                ) || e.eventAt().toLocalTime().isBefore(
                        LocalTime.of(
                                12,
                                59
                        )
                )
                ,
                "Shift2");

        for (EntrySummaryDTO entry :
                firstPage) {
            if (entry.shift() == null || entry.shift().isEmpty()) continue;
            assertThat(entry.shift().get(0).logbook().id()).isEqualTo(logbook.id());
        }

        //check all shift
        assertThat(firstPage)
                .filteredOn(entry -> entry.shift() == null || entry.shift().isEmpty())
                .filteredOn(not(outOfShift))
                .hasSize(0);
        assertThat(firstPage)
                // select only shift 1
                .filteredOn(entry -> entry.shift() != null || entry.shift().get(0).name().compareTo("Shift1") == 0)
                // remove shift 1
                .filteredOn(not(shift1))
                .hasSize(0);
        assertThat(firstPage)
                // select only shift 1
                .filteredOn(entry -> entry.shift() != null || entry.shift().get(0).name().compareTo("Shift2") == 0)
                // remove shift 1
                .filteredOn(not(shift2))
                .hasSize(0);
        // check summary against full entry
        for (EntrySummaryDTO es :
                firstPage) {
            EntryDTO fullEntry = entryService.getFullEntry(
                    es.id()
            );
            assertThat(fullEntry).isNotNull();
            assertThat(fullEntry.shifts()).isEqualTo(es.shift());
        }
    }

    @Test
    public void testSummarizationFailWrongShiftAndDate() {
        var logbookTest = getTestLogbook();

        //try to save a summary without any shift on logbooks
        ControllerLogicException exceptionOnNoShiftOnLogbook = assertThrows(
                ControllerLogicException.class,
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .title("title")
                                .text("text")
                                .logbooks(List.of(logbookTest.id()))
                                .summarizes(
                                        SummarizesDTO
                                                .builder()
                                                .build()
                                )
                                .build()
                )
        );

        assertThat(exceptionOnNoShiftOnLogbook.getErrorCode()).isEqualTo(-1);

        assertDoesNotThrow(
                () -> {
                    logbookService.replaceShift(
                            logbookTest.id(),
                            List.of(
                                    ShiftDTO
                                            .builder()
                                            .name("Shift1")
                                            .from(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    0,
                                                                    0
                                                            )
                                                    )
                                            )
                                            .to(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    7,
                                                                    59
                                                            )
                                                    )
                                            )
                                            .build()
                            )
                    );
                    return null;
                }
        );

        //try to save a summary
        ControllerLogicException exceptionOnNoShiftName = assertThrows(
                ControllerLogicException.class,
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .title("title")
                                .text("text")
                                .logbooks(List.of(logbookTest.id()))
                                .summarizes(
                                        SummarizesDTO
                                                .builder()
                                                .build()
                                )
                                .build()
                )
        );

        assertThat(exceptionOnNoShiftName.getErrorCode()).isEqualTo(-2);

        ControllerLogicException exceptionOnNoDate = assertThrows(
                ControllerLogicException.class,
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .title("title")
                                .text("text")
                                .logbooks(List.of(logbookTest.id()))
                                .summarizes(
                                        SummarizesDTO
                                                .builder()
                                                .shiftId("wrong id")
                                                .build()
                                )
                                .build()
                )
        );

        assertThat(exceptionOnNoDate.getErrorCode()).isEqualTo(-3);

        ShiftNotFound exceptionOnNotFoundShiftName = assertThrows(
                ShiftNotFound.class,
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .title("title")
                                .text("text")
                                .logbooks(List.of(logbookTest.id()))
                                .summarizes(
                                        SummarizesDTO
                                                .builder()
                                                .shiftId("wrong id")
                                                .date(LocalDate.now())
                                                .build()
                                )
                                .build()
                )
        );

        assertThat(exceptionOnNotFoundShiftName.getErrorCode()).isEqualTo(-4);
    }

    @Test
    public void testSummarization() {
        var logbookTest = getTestLogbook();
        assertDoesNotThrow(
                () -> {
                    logbookService.replaceShift(
                            logbookTest.id(),
                            List.of(
                                    ShiftDTO
                                            .builder()
                                            .name("Shift1")
                                            .from(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    0,
                                                                    0
                                                            )
                                                    )
                                            )
                                            .to(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    7,
                                                                    59
                                                            )
                                                    )
                                            )
                                            .build(),
                                    ShiftDTO
                                            .builder()
                                            .name("Shift2")
                                            .from(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    8,
                                                                    0
                                                            )
                                                    )
                                            )
                                            .to(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    12,
                                                                    59
                                                            )
                                                    )
                                            )
                                            .build(),
                                    ShiftDTO
                                            .builder()
                                            .name("Shift3")
                                            .from(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    13,
                                                                    0
                                                            )
                                                    )
                                            )
                                            .to(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    17,
                                                                    59
                                                            )
                                                    )
                                            )
                                            .build()
                            )
                    );
                    return null;
                }
        );

        LogbookDTO lb = assertDoesNotThrow(
                () -> logbookService.getLogbookByName(
                        logbookTest.name()
                )
        );

        //try to save a summary
        String entryID = assertDoesNotThrow(
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .title("title")
                                .text("text")
                                .logbooks(List.of(logbookTest.id()))
                                .summarizes(
                                        SummarizesDTO
                                                .builder()
                                                .shiftId(lb.shifts().get(0).id())
                                                .date(LocalDate.now())
                                                .build()
                                )
                                .build()
                )
        );

        assertThat(entryID).isNotNull().isNotEmpty();
    }

    @Test
    public void testSearchFilteringOnSummaries() {
        var logbookTest = getTestLogbook();
        assertDoesNotThrow(
                () -> {
                    logbookService.replaceShift(
                            logbookTest.id(),
                            List.of(
                                    ShiftDTO
                                            .builder()
                                            .name("Shift1")
                                            .from(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    0,
                                                                    0
                                                            )
                                                    )
                                            )
                                            .to(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    7,
                                                                    59
                                                            )
                                                    )
                                            )
                                            .build(),
                                    ShiftDTO
                                            .builder()
                                            .name("Shift2")
                                            .from(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    8,
                                                                    0
                                                            )
                                                    )
                                            )
                                            .to(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    12,
                                                                    59
                                                            )
                                                    )
                                            )
                                            .build(),
                                    ShiftDTO
                                            .builder()
                                            .name("Shift3")
                                            .from(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    13,
                                                                    0
                                                            )
                                                    )
                                            )
                                            .to(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    17,
                                                                    59
                                                            )
                                                    )
                                            )
                                            .build()
                            )
                    );
                    return null;
                }
        );
        LogbookDTO lb = assertDoesNotThrow(
                () -> logbookService.getLogbookByName(
                        logbookTest.name()
                )
        );

        //try to save a summary
        String entryID = assertDoesNotThrow(
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .title("title summary")
                                .text("text summary")
                                .logbooks(List.of(logbookTest.id()))
                                .summarizes(
                                        SummarizesDTO
                                                .builder()
                                                .shiftId(lb.shifts().get(0).id())
                                                .date(LocalDate.now())
                                                .build()
                                )
                                .build()
                )
        );
        assertThat(entryID).isNotNull().isNotEmpty();

        entryID = assertDoesNotThrow(
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .title("title")
                                .text("text")
                                .logbooks(List.of(logbookTest.id()))
                                .build()
                )
        );
        assertThat(entryID).isNotNull().isNotEmpty();

        List<EntrySummaryDTO> found = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .limit(10)
                                .build()
                )
        );
        assertThat(found).isNotNull().hasSize(2);

        found = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .hideSummaries(true)
                                .limit(10)
                                .build()
                )
        );
        assertThat(found).isNotNull().hasSize(1);
    }

    @Test
    public void findSummaryId() {
        var logbookTest = getTestLogbook();
        assertDoesNotThrow(
                () -> {
                    logbookService.replaceShift(
                            logbookTest.id(),
                            List.of(
                                    ShiftDTO
                                            .builder()
                                            .name("Shift1")
                                            .from(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    0,
                                                                    0
                                                            )
                                                    )
                                            )
                                            .to(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    7,
                                                                    59
                                                            )
                                                    )
                                            )
                                            .build(),
                                    ShiftDTO
                                            .builder()
                                            .name("Shift2")
                                            .from(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    8,
                                                                    0
                                                            )
                                                    )
                                            )
                                            .to(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    12,
                                                                    59
                                                            )
                                                    )
                                            )
                                            .build(),
                                    ShiftDTO
                                            .builder()
                                            .name("Shift3")
                                            .from(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    13,
                                                                    0
                                                            )
                                                    )
                                            )
                                            .to(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    17,
                                                                    59
                                                            )
                                                    )
                                            )
                                            .build()
                            )
                    );
                    return null;
                }
        );
        LogbookDTO lb = assertDoesNotThrow(
                () -> logbookService.getLogbookByName(
                        logbookTest.name()
                )
        );
        //try to save a summary
        String entryID1 = assertDoesNotThrow(
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .title("title summary")
                                .text("text summary")
                                .logbooks(List.of(logbookTest.id()))
                                .summarizes(
                                        SummarizesDTO
                                                .builder()
                                                .shiftId(lb.shifts().get(0).id())
                                                .date(LocalDate.now())
                                                .build()
                                )
                                .build()
                )
        );
        assertThat(entryID1).isNotNull().isNotEmpty();
        String entryID2 = assertDoesNotThrow(
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .title("title summary")
                                .text("text summary")
                                .logbooks(List.of(logbookTest.id()))
                                .summarizes(
                                        SummarizesDTO
                                                .builder()
                                                .shiftId(lb.shifts().get(0).id())
                                                .date(LocalDate.now().minusDays(1))
                                                .build()
                                )
                                .build()
                )
        );
        assertThat(entryID2).isNotNull().isNotEmpty();

        String idFound1 = assertDoesNotThrow(
                () -> entryService.findSummaryIdForShiftIdAndDate(
                        lb.shifts().get(0).id(),
                        LocalDate.now()
                )
        );
        assertThat(idFound1).isNotNull().isNotEmpty().isEqualTo(entryID1);

        String idFound2 = assertDoesNotThrow(
                () -> entryService.findSummaryIdForShiftIdAndDate(
                        lb.shifts().get(0).id(),
                        LocalDate.now().minusDays(1)
                )
        );
        assertThat(idFound2).isNotNull().isNotEmpty().isEqualTo(entryID2);
    }

    @Test
    public void failingDeletingShiftAssociatedToASummary() {
        var logbookTest = getTestLogbook();
        assertDoesNotThrow(
                () -> {
                    logbookService.replaceShift(
                            logbookTest.id(),
                            List.of(
                                    ShiftDTO
                                            .builder()
                                            .name("Shift1")
                                            .from(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    0,
                                                                    0
                                                            )
                                                    )
                                            )
                                            .to(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    7,
                                                                    59
                                                            )
                                                    )
                                            )
                                            .build(),
                                    ShiftDTO
                                            .builder()
                                            .name("Shift2")
                                            .from(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    8,
                                                                    0
                                                            )
                                                    )
                                            )
                                            .to(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    12,
                                                                    59
                                                            )
                                                    )
                                            )
                                            .build(),
                                    ShiftDTO
                                            .builder()
                                            .name("Shift3")
                                            .from(DateUtilities.toUTCString(
                                                    LocalTime.of(
                                                            13,
                                                            0
                                                    )
                                            ))
                                            .to(
                                                    DateUtilities.toUTCString(
                                                            LocalTime.of(
                                                                    17,
                                                                    59
                                                            )
                                                    )
                                            )
                                            .build()
                            )
                    );
                    return null;
                }
        );
        LogbookDTO lb = assertDoesNotThrow(
                () -> logbookService.getLogbookByName(
                        logbookTest.name()
                )
        );
        assertThat(lb.shifts()).isNotNull().hasSize(3);
        //try to save a summary
        String entryID = assertDoesNotThrow(
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .title("title summary")
                                .text("text summary")
                                .logbooks(List.of(logbookTest.id()))
                                .summarizes(
                                        SummarizesDTO
                                                .builder()
                                                .shiftId(lb.shifts().get(1).id())
                                                .date(LocalDate.now())
                                                .build()
                                )
                                .build()
                )
        );
        assertThat(entryID).isNotNull().isNotEmpty();

        //try to delete the shift used by the summary
        ControllerLogicException deleteException = assertThrows(
                ControllerLogicException.class,
                () -> {
                    logbookService.replaceShift(
                            logbookTest.id(),
                            List.of(
                                    ShiftDTO
                                            .builder()
                                            .id(lb.shifts().get(2).id())
                                            .name("Shift3Modified")
                                            .from("13:00")
                                            .to("17:59")
                                            .build()
                            )
                    );
                }
        );

        assertThat(deleteException.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void failingDeletingTagsAssociatedToASummary() {
        var logbookTest = getTestLogbook();
        LogbookDTO logbookTestUpdated = assertDoesNotThrow(
                () -> logbookService.update(
                        logbookTest.id(),
                        UpdateLogbookDTO
                                .builder()
                                .name(logbookTest.name())
                                .shifts(
                                        emptyList()
                                )
                                .tags(
                                        List.of(
                                                TagDTO
                                                        .builder()
                                                        .name("tag1")
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(logbookTestUpdated).isNotNull();
        //try to save a summary
        String entryID = assertDoesNotThrow(
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .title("title summary")
                                .text("text summary")
                                .logbooks(List.of(logbookTest.id()))
                                .tags(
                                        List.of(
                                                logbookTestUpdated.tags().get(0).id()
                                        )
                                )
                                .build()
                )
        );
        assertThat(entryID).isNotNull().isNotEmpty();

        //try to delete the shift used by the summary
        ControllerLogicException failForDeleteAssignedTag = assertThrows(
                ControllerLogicException.class,
                () -> {
                    logbookService.update(
                            logbookTest.id(),
                            UpdateLogbookDTO
                                    .builder()
                                    .name(logbookTest.name())
                                    .shifts(
                                            emptyList()
                                    )
                                    .tags(
                                            List.of(
                                                    TagDTO
                                                            .builder()
                                                            .name("tag2")
                                                            .build()
                                            )
                                    )
                                    .build()
                    );
                }
        );

        assertThat(failForDeleteAssignedTag.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void testReferencesOk() {
        var logbook = getTestLogbook();
        String referencedEntryId = assertDoesNotThrow(
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .logbooks(List.of(logbook.id()))
                                .title("Referenced entry")
                                .text("This is a log for a referenced entry")
                                .build()
                )
        );
        assertThat(referencedEntryId).isNotNull();

        String referencerEntryId = assertDoesNotThrow(
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .logbooks(List.of(logbook.id()))
                                .title("New entry")
                                .text("This is a text with reference in a link <a href=\"http://test.com/%s\">Reference link</a>".formatted(referencedEntryId))
                                .build()
                )
        );
        assertThat(referencerEntryId).isNotNull();

        //fetch referencer
        EntryDTO referencerEntry = assertDoesNotThrow(
                () -> entryService.getFullEntry(
                        referencerEntryId,
                        Optional.of(false),
                        Optional.of(false),
                        Optional.of(false),
                        Optional.of(true),
                        Optional.of(true)

                )
        );

        assertThat(referencerEntry.references()).hasSize(1).contains(referencedEntryId);

        //fetch referenced
        EntryDTO referencedEntry = assertDoesNotThrow(
                () -> entryService.getFullEntry(
                        referencedEntryId,
                        Optional.of(false),
                        Optional.of(false),
                        Optional.of(false),
                        Optional.of(true),
                        Optional.of(true)
                )
        );

        assertThat(referencedEntry.referencedBy()).hasSize(1).contains(referencerEntryId);
    }

    @Test
    public void testReferencesFailsOnBadReferenceId() {
        var logbook = getTestLogbook();

        String newEntryId = assertDoesNotThrow(
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .logbooks(List.of(logbook.id()))
                                .title("New entry")
                                .text("This is a text with reference in a link <a href=\"http://test.com/%s\">Reference link</a>".formatted("bad-id"))
                                .build()
                )
        );
        assertThat(newEntryId).isNotNull();

        EntryDTO newEntry = assertDoesNotThrow(
                () -> entryService.getFullEntry(
                        newEntryId,
                        Optional.of(false),
                        Optional.of(false),
                        Optional.of(false),
                        Optional.of(true),
                        Optional.of(true)
                )
        );

        assertThat(newEntry.references()).hasSize(0);
    }

    @Test
    public void testReferencesOnFindHidingSupersededOneOk() {
        var logbook = getTestLogbook();
        String referencedEntryId = assertDoesNotThrow(
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .logbooks(List.of(logbook.id()))
                                .title("Referenced entry")
                                .text("This is a log for a referenced entry")
                                .build()
                )
        );
        assertThat(referencedEntryId).isNotNull();

        String referencerEntryId = assertDoesNotThrow(
                () -> entryService.createNew(
                        EntryNewDTO
                                .builder()
                                .logbooks(List.of(logbook.id()))
                                .title("New entry")
                                .text("This is a text with reference in a link <a href=\"http://test.com/%s\">Reference link</a>".formatted(referencedEntryId))
                                .build()
                )
        );
        assertThat(referencerEntryId).isNotNull();


        // now supersede the entry that reference the first one
        String referencerSupersedeEntryId = assertDoesNotThrow(
                () -> entryService.createNewSupersede(
                        referencerEntryId,
                        EntryNewDTO
                                .builder()
                                .logbooks(List.of(logbook.id()))
                                .title("New entry that supersede the referencer one")
                                .text("This is a text with reference in a link <a href=\"http://test.com/%s\" data-references-entry=\"%s\">Reference link</a>".formatted(referencedEntryId, referencedEntryId))
                                .build()
                )
        );
        assertThat(referencerSupersedeEntryId).isNotNull();

        //find entry
        EntryDTO referencedEntry = assertDoesNotThrow(
                () -> entryService.getFullEntry(
                        referencedEntryId,
                        Optional.of(false),
                        Optional.of(false),
                        Optional.of(false),
                        Optional.of(true),
                        Optional.of(true)
                )
        );

        EntryDTO referencerEntry = assertDoesNotThrow(
                () -> entryService.getFullEntry(
                        referencerSupersedeEntryId,
                        Optional.of(false),
                        Optional.of(false),
                        Optional.of(false),
                        Optional.of(true),
                        Optional.of(true)
                )
        );

        assertThat(referencerEntry.references()).hasSize(1).contains(referencedEntryId);
        assertThat(referencedEntry.referencedBy()).hasSize(1).contains(referencerSupersedeEntryId);
    }
}
