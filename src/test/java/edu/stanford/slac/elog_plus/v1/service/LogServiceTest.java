package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.model.Log;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import edu.stanford.slac.elog_plus.service.LogService;
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class LogServiceTest {
    @Autowired
    private LogService logService;
    @Autowired
    private AttachmentService attachmentService;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Log.class);
    }

    @Test
    public void testLogCreation() {
        String newLogID = logService.createNew(
                NewLogDTO
                        .builder()
                        .logbook("MCC")
                        .text("This is a log for test")
                        .title("A very wonderful log")
                        .build()
        );

        QueryPagedResultDTO<SearchResultLogDTO> queryResult =
                assertDoesNotThrow(
                        () -> logService.searchAll(
                                QueryParameterDTO
                                        .builder()
                                        .logBook(List.of("MCC"))
                                        .build()
                        )
                );

        assertThat(queryResult.getTotalElements()).isEqualTo(1);
        assertThat(queryResult.getContent().get(0).id()).isEqualTo(newLogID);
    }

    @Test
    public void testFetchFullLog() {
        String newLogID =
                assertDoesNotThrow(
                        () -> logService.createNew(
                                NewLogDTO
                                        .builder()
                                        .logbook("MCC")
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );

        LogDTO fullLog =
                assertDoesNotThrow(
                        () -> logService.getFullLog(newLogID)
                );
        assertThat(fullLog.id()).isEqualTo(newLogID);
    }

    @Test
    public void testSupersedeCreationFailOnWrongRootLog() {
        ControllerLogicException exception = assertThrows(
                ControllerLogicException.class,
                () -> logService.createNewSupersede(
                        "bad id",
                        NewLogDTO
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
                        () -> logService.createNew(
                                NewLogDTO
                                        .builder()
                                        .logbook("MCC")
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .build()
                        )
                );

        String newLogID =
                assertDoesNotThrow(
                        () -> logService.createNewSupersede(
                                supersededLogID,
                                NewLogDTO
                                        .builder()
                                        .logbook("MCC")
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for supersede")
                                        .build()
                        )
                );

        LogDTO supersededLog = assertDoesNotThrow(
                () -> logService.getFullLog(supersededLogID)
        );

        assertThat(supersededLog).isNotNull();
        assertThat(supersededLog.supersedeBy()).isEqualTo(newLogID);

        LogDTO fullLog = assertDoesNotThrow(
                () -> logService.getFullLog(newLogID)
        );

        assertThat(fullLog).isNotNull();
        assertThat(fullLog.id()).isEqualTo(newLogID);
    }

    @Test
    public void testSupersedeErrorOnDoubleSuperseding() {
        String supersededLogID =
                assertDoesNotThrow(
                        () -> logService.createNew(
                                NewLogDTO
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
                        () -> logService.createNewSupersede(
                                supersededLogID,
                                NewLogDTO
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
                        () -> logService.createNewSupersede(
                                supersededLogID,
                                NewLogDTO
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
                () -> logService.createNewFollowUp(
                        "bad root id",
                        NewLogDTO
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
                        () -> logService.createNew(
                                NewLogDTO
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
                        () -> logService.createNewFollowUp(
                                rootLogID,
                                NewLogDTO
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
                        () -> logService.createNewFollowUp(
                                rootLogID,
                                NewLogDTO
                                        .builder()
                                        .logbook("MCC")
                                        .text("This is a log for test")
                                        .title("A very wonderful log updated for followUp two")
                                        .build()
                        )
                );
        assertThat(newFollowUpTwoID).isNotNull();

        List<SearchResultLogDTO> followUpLogsFound =
                assertDoesNotThrow(
                        () -> logService.getAllFollowUpForALog(
                                rootLogID
                        )
                );

        assertThat(followUpLogsFound).isNotNull();
        assertThat(followUpLogsFound.size()).isEqualTo(2);
    }

    @Test
    public void testLogAttachmentOnFullDTO() {
        String attachmentID =
                assertDoesNotThrow(
                        () -> attachmentService.createAttachment(
                                FileObjectDescription
                                        .builder()
                                        .fileName("fileName")
                                        .contentType("content-type")
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
                        () -> logService.createNew(
                                NewLogDTO
                                        .builder()
                                        .logbook("MCC")
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .attachments(List.of(attachmentID))
                                        .build()
                        )
                );

        // check for the attachment are well filled into dto
        LogDTO foundLog =
                assertDoesNotThrow(
                        () -> logService.getFullLog(newLogID)
                );
        assertThat(foundLog).isNotNull();
        assertThat(foundLog.attachments().size()).isEqualTo(1);
        assertThat(foundLog.attachments().get(0).fileName()).isEqualTo("fileName");
        assertThat(foundLog.attachments().get(0).contentType()).isEqualTo("content-type");
    }

    @Test
    public void testLogAttachmentOnSearchDTO() {
        String attachmentID =
                assertDoesNotThrow(
                        () -> attachmentService.createAttachment(
                                FileObjectDescription
                                        .builder()
                                        .fileName("fileName")
                                        .contentType("content-type")
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
                        () -> logService.createNew(
                                NewLogDTO
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
                        () -> logService.searchAll(
                                QueryParameterDTO
                                        .builder()
                                        .build()
                        )
                );
        assertThat(foundLog).isNotNull();
        assertThat(foundLog.getContent().size()).isEqualTo(1);
        assertThat(foundLog.getContent().get(0).attachments().size()).isEqualTo(1);
        assertThat(foundLog.getContent().get(0).attachments().get(0).fileName()).isEqualTo("fileName");
        assertThat(foundLog.getContent().get(0).attachments().get(0).contentType()).isEqualTo("content-type");
    }
}
