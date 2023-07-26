package edu.stanford.slac.elog_plus.v1.service;

import com.github.javafaker.Faker;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.exception.EntryNotFound;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import edu.stanford.slac.elog_plus.service.EntryService;
import edu.stanford.slac.elog_plus.service.LogbookService;
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

import static org.assertj.core.api.AssertionsForClassTypes.anyOf;
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
                        .logbook(logbook.name())
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
                        .logbook(logbook.name())
                        .text("<h1>H1</h1><h2>H2</h2><p><a href='http://example.com/' onclick='stealCookies()'>Link</a></p>")
                        .title("A very wonderful log")
                        .build()
        );

        EntryDTO fullLog =
                assertDoesNotThrow(
                        () -> entryService.getFullLog(
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
                                        .logbook(logbook.name())
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
                        () -> entryService.getFullLog("wrong id")
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
                                        .logbook(logbook.name())
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );

        EntryDTO fullLog =
                assertDoesNotThrow(
                        () -> entryService.getFullLog(newLogID)
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
                                        .logbook(logbook.name())
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
                        () -> entryService.getFullLog(newLogID)
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
                                        .logbook(logbook.name())
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );

        EntryDTO fullLog =
                assertDoesNotThrow(
                        () -> entryService.getFullLog(newLogID)
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
                                .logbook(logbook.name())
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
                                        .logbook(logbook.name())
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
                                        .logbook(logbook.name())
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for supersede")
                                        .build()
                        )
                );

        EntryDTO supersededLog = assertDoesNotThrow(
                () -> entryService.getFullLog(supersededLogID)
        );

        assertThat(supersededLog).isNotNull();
        assertThat(supersededLog.supersedeBy()).isEqualTo(newLogID);

        EntryDTO fullLog = assertDoesNotThrow(
                () -> entryService.getFullLog(newLogID)
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
                                        .logbook(logbook.name())
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
                                        .logbook(logbook.name())
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
                                        .logbook(logbook.name())
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
                                        .logbook(logbook.name())
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
                                        .logbook(logbook.name())
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
                                        .logbook(logbook.name())
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
                                .logbook(logbook.name())
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
                                        .logbook(logbook.name())
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
                                        .logbook(logbook.name())
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
                                        .logbook(logbook.name())
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
                                        .logbook(logbook.name())
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
                                        .logbook(logbook.name())
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for followUps one")
                                        .build()
                        )
                );
        assertThat(newFollowUpOneID).isNotNull();

        EntryDTO logWithFlowingUpLog =
                assertDoesNotThrow(
                        () -> entryService.getFullLog(
                                newFollowUpOneID,
                                Optional.empty(),
                                Optional.of(true),
                                Optional.empty()
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
                                        .logbook(logbook.name())
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .attachments(List.of(attachmentID))
                                        .build()
                        )
                );

        // check for the attachment are well filled into dto
        EntryDTO foundLog =
                assertDoesNotThrow(
                        () -> entryService.getFullLog(newLogID)
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
                                        .logbook(logbook.name())
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
                                            .logbook(logbook.name())
                                            .text("This is a log for test")
                                            .title("A very wonderful log")
                                            .segment(String.valueOf(finalIdx))
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
        String lastSegment = null;
        for (EntrySummaryDTO l :
                firstPage) {
            if (lastSegment == null) {
                lastSegment = l.segment();
                continue;
            }
            assertThat(Integer.valueOf(lastSegment)).isGreaterThan(
                    Integer.valueOf(l.segment())
            );
            lastSegment = l.segment();
        }
        var testAnchorLog = firstPage.get(firstPage.size() - 1);
        // load next page
        List<EntrySummaryDTO> nextPage = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .startDate(testAnchorLog.loggedAt())
                                .limit(10)
                                .build()
                )
        );

        assertThat(nextPage).isNotNull();
        assertThat(nextPage.size()).isEqualTo(10);
        // check that the first of next page is not the last of the previous
        assertThat(nextPage.get(0).id()).isNotEqualTo(firstPage.get(firstPage.size() - 1).id());

        lastSegment = nextPage.get(0).segment();
        nextPage.remove(0);
        for (EntrySummaryDTO l :
                nextPage) {
            assertThat(Integer.valueOf(lastSegment)).isGreaterThan(
                    Integer.valueOf(l.segment())
            );
            lastSegment = l.segment();
        }

        // now get all the record upside and downside
        List<EntrySummaryDTO> prevPageByPin = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .endDate(testAnchorLog.loggedAt())
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
                                .endDate(testAnchorLog.loggedAt())
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
        EntryDTO middleAnchorLog = entryService.getFullLog(anchorID);
        List<EntrySummaryDTO> prevAndNextPageByMiddlePin = assertDoesNotThrow(
                () -> entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .endDate(middleAnchorLog.loggedAt())
                                .contextSize(10)
                                .limit(10)
                                .build()
                )
        );
        assertThat(prevAndNextPageByMiddlePin).isNotNull();
        assertThat(prevAndNextPageByMiddlePin.size()).isEqualTo(20);
        assertThat(prevAndNextPageByMiddlePin.get(0).segment()).isEqualTo("58");
        assertThat(prevAndNextPageByMiddlePin.get(19).segment()).isEqualTo("39");
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
                                .from("00:00")
                                .to("07:59")
                                .build(),
                        ShiftDTO
                                .builder()
                                .name("Shift2")
                                .from("08:00")
                                .to("12:59")
                                .build(),
                        ShiftDTO
                                .builder()
                                .name("Shift3")
                                .from("13:00")
                                .to("17:59")
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
                                            .logbook(logbook.name())
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

        Condition<EntrySummaryDTO> shift1Condition = new Condition<>(
                e -> e.shift() != null && e.shift().compareTo("Shift1") == 0 &&
                        e.eventAt().toLocalTime().equals(
                                LocalTime.of(
                                        0,
                                        0
                                )
                        ) || (
                            e.eventAt().toLocalTime().isAfter(
                                    LocalTime.of(
                                            0,
                                            0
                                    )
                            ) &&
                                e.eventAt().toLocalTime().isBefore(
                                        LocalTime.of(
                                                7,
                                                59
                                        )
                                )
                        ) ||
                        e.eventAt().toLocalTime().equals(
                                LocalTime.of(
                                        7,
                                        59
                                )
                        ),
                "Shift1"
        );
        Condition<EntrySummaryDTO> shift2Condition = new Condition<>(
                e -> e.shift() != null && e.shift().compareTo("Shift2") == 0 &&
                        e.eventAt().toLocalTime().equals(
                                LocalTime.of(
                                        8,
                                        0
                                )
                        ) ||
                        (e.eventAt().toLocalTime().isAfter(
                                LocalTime.of(
                                        8,
                                        0
                                )
                        ) &&
                        e.eventAt().toLocalTime().isBefore(
                                LocalTime.of(
                                        12,
                                        59
                                )
                        )) ||
                        e.eventAt().toLocalTime().equals(
                                LocalTime.of(
                                        12,
                                        59
                                )
                        ),
                "Shift2"
        );
        Condition<EntrySummaryDTO> shift3Condition = new Condition<>(
                e -> e.shift() != null && e.shift().compareTo("Shift3") == 0 &&
                        e.eventAt().toLocalTime().equals(
                                LocalTime.of(
                                        13,
                                        0
                                )
                        ) ||
                        (e.eventAt().toLocalTime().isAfter(
                                LocalTime.of(
                                        13,
                                        0
                                )
                        ) &&
                        e.eventAt().toLocalTime().isBefore(
                                LocalTime.of(
                                        17,
                                        59
                                )
                        )) ||
                        e.eventAt().toLocalTime().equals(
                                LocalTime.of(
                                        17,
                                        59
                                )
                        ),
                "Shift3"
        );
        Condition<EntrySummaryDTO> shiftNullCondition = new Condition<>(
                e -> e.shift() == null &&
                        e.eventAt().toLocalTime().equals(
                                LocalTime.of(
                                        18,
                                        0
                                )
                        ) ||
                        (e.eventAt().toLocalTime().isAfter(
                                LocalTime.of(
                                        18,
                                        0
                                )
                        ) &&
                        e.eventAt().toLocalTime().isBefore(
                                LocalTime.of(
                                        23,
                                        59
                                )
                        )) ||
                        e.eventAt().toLocalTime().equals(
                                LocalTime.of(
                                        23,
                                        59
                                )
                        ),
                "Shift2"
        );

        //check all shift
        assertThat(firstPage)
                .filteredOn
                        (
                                anyOf(
                                        shift1Condition,
                                        shift2Condition,
                                        shift3Condition,
                                        shiftNullCondition
                                )
                        ).hasSize(30);

        // check summary against full entry

        for (EntrySummaryDTO es:
             firstPage) {
            EntryDTO fullEntry = entryService.getFullLog(
                    es.id()
            );
            assertThat(fullEntry).isNotNull();
            assertThat(fullEntry.shift()).isEqualTo(es.shift());
        }
    }
}
