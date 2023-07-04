package edu.stanford.slac.elog_plus.v1.controller;

import com.github.javafaker.Faker;
import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.LogDTO;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.service.AttachmentService;
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
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class AttachmentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TestHelperService testHelperService;

    @Autowired
    AttachmentService attachmentService;

    @Autowired
    DocumentGenerationService documentGenerationService;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Attachment.class);
    }

    @Test
    public void createAttachment() throws Exception {
        ApiResultResponse<String> newAttachmentID = testHelperService.newAttachment(
                mockMvc,
                new MockMultipartFile(
                        "uploadFile",
                        "contract.pdf",
                        MediaType.APPLICATION_PDF_VALUE,
                        "<<pdf data>>".getBytes(StandardCharsets.UTF_8)
                )
        );

        Attachment retrivedAttachment = mongoTemplate.findOne(
                new Query().addCriteria(
                        Criteria.where("id").is(newAttachmentID.getPayload())
                ),
                Attachment.class
        );

        AssertionsForClassTypes.assertThat(retrivedAttachment).isNotNull();
    }

    @Test
    public void downloadAttachment() throws Exception {
        Faker f = new Faker();
        String fileName = f.file().fileName();
        ApiResultResponse<String> newAttachmentID = testHelperService.newAttachment(
                mockMvc,
                new MockMultipartFile(
                        "uploadFile",
                        fileName,
                        MediaType.APPLICATION_PDF_VALUE,
                        "<<pdf data>>".getBytes(StandardCharsets.UTF_8)
                )
        );

        testHelperService.checkDownloadedFile(
                mockMvc,
                newAttachmentID.getPayload(),
                MediaType.APPLICATION_PDF_VALUE
        );
    }

    @Test
    public void downloadAttachmentAndPreview() throws Exception {
        try (InputStream is = documentGenerationService.getTestPng()) {
            ApiResultResponse<String> newAttachmentID = testHelperService.newAttachment(
                    mockMvc,
                    new MockMultipartFile(
                            "uploadFile",
                            "test.png",
                            MediaType.IMAGE_PNG_VALUE,
                            is
                    )
            );

            testHelperService.checkDownloadedFile(
                    mockMvc,
                    newAttachmentID.getPayload(),
                    MediaType.IMAGE_PNG_VALUE
            );

            await().atMost(10, SECONDS).pollDelay(500, MILLISECONDS).until(
                    () -> {
                        String processingState = attachmentService.getPreviewProcessingState(newAttachmentID.getPayload());
                        return processingState.compareTo(
                                        Attachment.PreviewProcessingState.Completed.name()
                                )==0;
                    }
            );

            testHelperService.checkDownloadedPreview(
                    mockMvc,
                    newAttachmentID.getPayload(),
                    MediaType.IMAGE_JPEG_VALUE
            );
        }

    }
}
