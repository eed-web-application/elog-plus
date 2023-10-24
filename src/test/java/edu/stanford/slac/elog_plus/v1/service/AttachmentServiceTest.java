package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.elog_plus.api.v1.dto.AttachmentDTO;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.repository.AttachmentRepository;
import edu.stanford.slac.elog_plus.repository.StorageRepository;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AttachmentServiceTest {
    private static final Logger log = LoggerFactory.getLogger(AttachmentServiceTest.class);
    @Autowired
    private S3Client s3Client;
    @Autowired
    private AttachmentService attachmentService;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private DocumentGenerationService documentGenerationService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private AppProperties appProperties;

    @BeforeEach
    public void preTest() {

        mongoTemplate.remove(new Query(), Attachment.class);
        ListObjectsV2Response objectListing = s3Client.listObjectsV2(
                ListObjectsV2Request
                        .builder()
                        .bucket(appProperties.getStorage().getBucket())
                        .build()

        );
        while (true) {
            objectListing.contents().forEach(
                    c -> s3Client.deleteObject(
                            DeleteObjectRequest
                                    .builder()
                                    .bucket(appProperties.getStorage().getBucket())
                                    .key(c.key())
                                    .build()
                    )
            );
            if (objectListing.isTruncated()) {
                objectListing = s3Client.listObjectsV2(
                        ListObjectsV2Request
                                .builder()
                                .bucket(appProperties.getStorage().getBucket())
                                .continuationToken(objectListing.nextContinuationToken())
                                .build()

                );
            } else {
                break;
            }
        }
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
                                log.info("state {} for attachment id {}", state, attachmentID);
                                return state.compareTo(Attachment.PreviewProcessingState.Completed.name()) == 0;
                            }
                    );

            AttachmentDTO attachment = assertDoesNotThrow(
                    () -> attachmentService.getAttachment(attachmentID)
            );
            assertThat(attachment.previewState()).isEqualTo(Attachment.PreviewProcessingState.Completed.name());

            var attachmentModel = attachmentRepository.findById(attachmentID);
            assertThat(attachmentModel.isPresent()).isTrue();
            assertThat(attachmentModel.get().getInUse()).isFalse();
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

    @Test
    public void testListObject() throws IOException {
        for (
                int idx = 0;
                idx < 10;
                idx++
        ) {
            try (InputStream is = assertDoesNotThrow(
                    () -> documentGenerationService.getTestJpeg()
            )) {
                // save the
                int finalIdx = idx;
                String attachmentID = assertDoesNotThrow(
                        () -> attachmentService.createAttachment(
                                FileObjectDescription
                                        .builder()
                                        .fileName("jpegFileName" + String.valueOf(finalIdx))
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

        // not try to find all 100 object into the object store
        var foundList1 = attachmentService.listFromStorage(10, null);
        assertThat(foundList1).isNotNull();
        assertThat(foundList1.continuationToken()).isNotNull();
        assertThat(foundList1.keyFounds()).hasSize(10);
        var foundList2 = attachmentService.listFromStorage(10, foundList1.continuationToken());
        assertThat(foundList2).isNotNull();
        assertThat(foundList2.continuationToken()).isNull();
        assertThat(foundList2.keyFounds()).hasSize(10);
        var foundList3 = attachmentService.listFromStorage(25, null);
        assertThat(foundList3).isNotNull();
        assertThat(foundList3.continuationToken()).isNull();
        assertThat(foundList3.keyFounds()).hasSize(20);
    }
}
