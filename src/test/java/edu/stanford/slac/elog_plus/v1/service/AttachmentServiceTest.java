package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.elog_plus.api.v1.dto.AttachmentDTO;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import edu.stanford.slac.elog_plus.service.EntryService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class AttachmentServiceTest {
    private static final Logger log = LoggerFactory.getLogger(AttachmentServiceTest.class);
    @Autowired
    private EntryService entryService;
    @Autowired
    private AttachmentService attachmentService;
    @Autowired
    DocumentGenerationService documentGenerationService;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {

        mongoTemplate.remove(new Query(), Attachment.class);
    }

    @Test
    public void testPreviewJpegOk() throws IOException {
        try (InputStream is = assertDoesNotThrow(
                () -> documentGenerationService.getTestJpeg()
        )) {
            //save the
            String attachmentID = assertDoesNotThrow(
                    () -> attachmentService.createAttachment(
                            FileObjectDescription
                                    .builder()
                                    .fileName("jpegFileName")
                                    .contentType(MediaType.IMAGE_JPEG_VALUE)
                                    .is(is)
                                    .build(),
                            true
                    )
            );

            await()
                    .atMost(30, SECONDS)
                    .pollDelay(2, SECONDS)
                    .until(
                    () -> {
                        String state = attachmentService.getPreviewProcessingState(attachmentID);
                        log.info("state {} for attachement id {}", state, attachmentID);
                        return state.compareTo(Attachment.PreviewProcessingState.Completed.name()) == 0;
                    }
            );

            AttachmentDTO attachment = assertDoesNotThrow(
                    () -> attachmentService.getAttachment(attachmentID)
            );
            assertThat(attachment.previewState()).isEqualTo(Attachment.PreviewProcessingState.Completed.name());
        }
    }

    @Test
    public void testPreviewPNGOk() throws IOException {
        try (InputStream is = assertDoesNotThrow(
                () -> documentGenerationService.getTestPng()
        )) {
            //save the
            String attachmentID = attachmentService.createAttachment(
                    FileObjectDescription
                            .builder()
                            .fileName("jpegFileName")
                            .contentType(MediaType.IMAGE_JPEG_VALUE)
                            .is(is)
                            .build(),
                    true
            );

            await().atMost(10, SECONDS).until(
                    () -> {
                        String state = attachmentService.getPreviewProcessingState(attachmentID);
                        log.info("state {} for attachment id {}", state, attachmentID);
                        return state.compareTo(Attachment.PreviewProcessingState.Completed.name()) == 0;
                    }
            );

            AttachmentDTO attachment = assertDoesNotThrow(
                    () -> attachmentService.getAttachment(attachmentID)
            );
            assertThat(attachment.previewState()).isEqualTo(Attachment.PreviewProcessingState.Completed.name());
        }
    }

    @Test
    public void testPdfPreviewUnavailable() throws IOException {
        try (PDDocument pdf = assertDoesNotThrow(
                () -> documentGenerationService.generatePdf()
        )) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pdf.save(baos);

            //save the
            String attachmentID = attachmentService.createAttachment(
                    FileObjectDescription
                            .builder()
                            .fileName("pdfFileName")
                            .contentType(MediaType.APPLICATION_PDF_VALUE)
                            .is(
                                    new ByteArrayInputStream(baos.toByteArray())
                            )
                            .build(),
                    true
            );

            await().atMost(10, SECONDS).until(
                    () -> {
                        String state = attachmentService.getPreviewProcessingState(attachmentID);
                        log.info("state {} for attachement id {}", state, attachmentID);
                        return state.compareTo(Attachment.PreviewProcessingState.PreviewNotAvailable.name()) == 0;
                    }
            );
        }
    }
}
