package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.elog_plus.api.v1.dto.LogDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewLogDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.QueryPagedResultDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.QueryParameterDTO;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.model.Attachment;
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
import org.springframework.http.MediaType;
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
        mongoTemplate.remove(new Query(), Attachment.class);
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

        QueryPagedResultDTO<LogDTO> queryResult =
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
    public void testLogCreationWithAttachment() {
        String attachmentID = assertDoesNotThrow(
                () -> attachmentService.createAttachment(
                        FileObjectDescription
                                .builder()
                                .is(
                                        new ByteArrayInputStream(
                                                "<<pdf data>>".getBytes(StandardCharsets.UTF_8)
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_PDF_VALUE
                                )
                                .fileName(
                                        "file"
                                )
                                .build()
                )
        );

        assertDoesNotThrow(
                () ->
                        logService.createNew(
                                NewLogDTO
                                        .builder()
                                        .logbook("MCC")
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .attachments(
                                                List.of(attachmentID)
                                        )
                                        .build()
                        )
        );


    }

    @Test
    public void testLogCreationFailNonExistingAttachment() {
        ControllerLogicException assertionResult = assertThrows(
                ControllerLogicException.class,
                () ->
                        logService.createNew(
                                NewLogDTO
                                        .builder()
                                        .logbook("MCC")
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .attachments(
                                                List.of("bad attachment id")
                                        )
                                        .build()
                        )
        );
        assertThat(assertionResult.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void testLogCreationFailOnAlreadyUsedAttachment() {
        String attachmentID = assertDoesNotThrow(
                () -> attachmentService.createAttachment(
                        FileObjectDescription
                                .builder()
                                .is(
                                        new ByteArrayInputStream(
                                                "<<pdf data>>".getBytes(StandardCharsets.UTF_8)
                                        )
                                )
                                .contentType(
                                        MediaType.APPLICATION_PDF_VALUE
                                )
                                .fileName(
                                        "file"
                                )
                                .build()
                )
        );

        assertDoesNotThrow(
                () ->
                        logService.createNew(
                                NewLogDTO
                                        .builder()
                                        .logbook("MCC")
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .attachments(
                                                List.of(attachmentID)
                                        )
                                        .build()
                        )
        );

        // need to fail because i use an already used attachment id
        ControllerLogicException assertionResult = assertThrows(
                ControllerLogicException.class,
                () ->
                        logService.createNew(
                                NewLogDTO
                                        .builder()
                                        .logbook("MCC")
                                        .text("This is a log for test")
                                        .title("A very wonderful log")
                                        .attachments(
                                                List.of(attachmentID)
                                        )
                                        .build()
                        )
        );
        assertThat(assertionResult.getErrorCode()).isEqualTo(-4);
    }
}
