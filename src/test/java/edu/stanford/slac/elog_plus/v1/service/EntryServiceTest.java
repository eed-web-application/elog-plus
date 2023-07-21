package edu.stanford.slac.elog_plus.v1.service;

import com.github.javafaker.Faker;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.exception.EntryNotFound;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import edu.stanford.slac.elog_plus.service.EntryService;
import edu.stanford.slac.elog_plus.service.LogbookService;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @BeforeAll
    public void initLogbook() {
        if(!logbookService.exist("MCC")) {
            logbookService.createNew(
                    NewLogbookDTO
                            .builder()
                            .name("MCC")
                            .build()
            );
        }
    }

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Entry.class);
    }

    @Test
    public void testLogCreation() {
        String newLogID = entryService.createNew(
                EntryNewDTO
                        .builder()
                        .logbook("MCC")
                        .text("This is a log for test")
                        .title("A very wonderful log")
                        .build()
        );

        assertThat(newLogID).isNotNull();
    }

    @Test
    public void testLogTextSanitization() {
        String newLogID = entryService.createNew(
                EntryNewDTO
                        .builder()
                        .logbook("MCC")
                        .text("<p><a href='http://example.com/' onclick='stealCookies()'>Link</a></p>")
                        .title("A very wonderful log")
                        .build()
        );

        EntryDTO fullLog =
                assertDoesNotThrow(
                        () -> entryService.getFullLog(
                                newLogID
                        )
                );
        assertThat(fullLog.text()).isEqualTo("<p><a href=\"http://example.com/\" rel=\"nofollow\">Link</a></p>");
    }

    @Test
    public void testFailBadTags() {
        ControllerLogicException ex =
                assertThrows(
                        ControllerLogicException.class,
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbook("MCC")
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .tags(List.of("wrong tags"))
                                        .build()
                        )
                );
        assertThat(ex.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void testFailBadAttachmentID() {
        ControllerLogicException ex =
                assertThrows(
                        ControllerLogicException.class,
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbook("MCC")
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
        String newLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbook("MCC")
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
                                        .logbook("MCC")
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
    public void testWitoutEventAtFetchFullLog() {
        String newLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbook("MCC")
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
        ControllerLogicException exception = assertThrows(
                ControllerLogicException.class,
                () -> entryService.createNewSupersede(
                        "bad id",
                        EntryNewDTO
                                .builder()
                                .logbook("MCC")
                                .text("This is a log for test")
                                .title("A very wonderful log")
                                .build()
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void testSupersedeOK() {
        String supersededLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbook("MCC")
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
                                        .logbook("MCC")
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
        String supersededLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbook("MCC")
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
                                        .logbook("MCC")
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
                                        .logbook("MCC")
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
        String supersededLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbook("MCC")
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
                                        .logbook("MCC")
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
                                        .logbook("MCC")
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for supersede one more time")
                                        .build()
                        )
                );
        assertThat(exception.getErrorCode()).isEqualTo(-3);
    }

    @Test
    public void testFollowUpCreationFailOnWrongRootLog() {
        ControllerLogicException exception = assertThrows(
                ControllerLogicException.class,
                () -> entryService.createNewFollowUp(
                        "bad root id",
                        EntryNewDTO
                                .builder()
                                .logbook("MCC")
                                .text("This is a log for test")
                                .title("A very wonderful log")
                                .build()
                )
        );

        assertThat(exception.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void testFollowUpCreation() {
        String rootLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbook("MCC")
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
                                        .logbook("MCC")
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for followUp one")
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
                                        .logbook("MCC")
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for followUp two")
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
        String rootLogID =
                assertDoesNotThrow(
                        () -> entryService.createNew(
                                EntryNewDTO
                                        .builder()
                                        .logbook("MCC")
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
                                        .logbook("MCC")
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for followUp one")
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
                                        .logbook("MCC")
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
                                        .logbook("MCC")
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
        String anchorID = null;
        // create some data
        for (int idx = 0; idx < 100; idx++) {
            int finalIdx = idx;
            String newLogID =
                    assertDoesNotThrow(
                            () -> entryService.createNew(
                                    EntryNewDTO
                                            .builder()
                                            .logbook("MCC")
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
}
