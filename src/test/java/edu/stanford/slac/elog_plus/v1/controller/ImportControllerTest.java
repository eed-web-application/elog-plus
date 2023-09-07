package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.LogbookService;
import edu.stanford.slac.elog_plus.v1.service.DocumentGenerationService;
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
import java.net.URISyntaxException;
import java.time.LocalDateTime;
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
public class ImportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    LogbookService logbookService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TestControllerHelperService testControllerHelperService;

    @Autowired
    DocumentGenerationService documentGenerationService;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Entry.class);
        mongoTemplate.remove(new Query(), Attachment.class);
        mongoTemplate.remove(new Query(), Logbook.class);
    }

    @Test
    public void uploadEntryFailsWithNoEntry() {
        MissingServletRequestPartException exception = assertThrows(
                MissingServletRequestPartException.class,
                () -> testControllerHelperService.uploadWholeEntry(
                        mockMvc, status().is4xxClientError(),
                        null,
                        new MockMultipartFile[]{}
                )
        );

        assertThat(exception.getMessage()).containsPattern(".*entry.*");
    }

    @Test
    public void uploadEntryWithNoAttachment() {
        var testLogbook = getTestLogbook();
        EntryImportDTO dto = EntryImportDTO
                .builder()
                .title("Sample Title")
                .text("sample text")
                .logbooks(List.of(testLogbook.getPayload().name()))
                .build();

        ApiResultResponse<String> uploadResult = assertDoesNotThrow(
                () -> testControllerHelperService.uploadWholeEntry(mockMvc, status().isCreated(), dto, new MockMultipartFile[]{})
        );

        assertThat(uploadResult.getErrorCode()).isEqualTo(0);
        assertThat(uploadResult.getPayload()).isNotEmpty();
    }

    @Test
    public void importEntryWithAttachment() {
        var testLogbook = getTestLogbook();
        EntryImportDTO dto = EntryImportDTO
                .builder()
                .originId("origin-id")
                .title("Sample Title")
                .text("sample text")
                .logbooks(List.of(testLogbook.getPayload().name()))
                .tags(
                        List.of(
                                "Tag OnE"
                        )
                )
                .firstName("First name import")
                .lastName("Last name import")
                .userName("Username name import")
                .eventAt(
                        LocalDateTime.of(
                                2021,
                                1,
                                1,
                                1,
                                1
                        )
                )
                .loggedAt(
                        LocalDateTime.of(
                                2021,
                                2,
                                1,
                                1,
                                1
                        )
                )
                .build();

        try {
            InputStream isPng = documentGenerationService.getTestPng();
            InputStream isJpg = documentGenerationService.getTestJpeg();
            ApiResultResponse<String> uploadResult = assertDoesNotThrow(
                    () -> testControllerHelperService.uploadWholeEntry(
                            mockMvc,
                            status().isCreated(),
                            dto,
                            new MockMultipartFile(
                                    "files",
                                    "test.png",
                                    MediaType.IMAGE_PNG_VALUE,
                                    isPng
                            ),
                            new MockMultipartFile(
                                    "files",
                                    "test.jpg",
                                    MediaType.IMAGE_JPEG_VALUE,
                                    isJpg
                            ))
            );

            assertThat(uploadResult).isNotNull();
            assertThat(uploadResult.getErrorCode()).isEqualTo(0);

            ApiResultResponse<EntryDTO> fullLog = assertDoesNotThrow(
                    () -> testControllerHelperService.getFullLog(
                            mockMvc,
                            status().isOk(),
                            uploadResult.getPayload()
                    )
            );
            assertThat(fullLog.getPayload().tags()).extracting("name").contains("tag-one");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }


        ControllerLogicException alreadyFound = assertThrows(
                ControllerLogicException.class,
                () -> testControllerHelperService.uploadWholeEntry(
                        mockMvc,
                        status().is5xxServerError(),
                        EntryImportDTO
                                .builder()
                                .originId("origin-id")
                                .title("Sample Title")
                                .text("sample text")
                                .logbooks(List.of(testLogbook.getPayload().name()))
                                .build()
                )
        );

        assertThat(alreadyFound.getErrorCode()).isEqualTo(-2);
    }

    private ApiResultResponse<LogbookDTO> getTestLogbook() {
        return getTestLogbook("user1@slac.stanford.edu");
    }

    private ApiResultResponse<LogbookDTO> getTestLogbook(String withUserEmail) {
        ApiResultResponse<String> logbookCreationResult = assertDoesNotThrow(
                () -> testControllerHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        Optional.of(
                                withUserEmail
                        ),
                        NewLogbookDTO
                                .builder()
                                .name(UUID.randomUUID().toString())
                                .build()
                ));
        assertThat(logbookCreationResult).isNotNull();
        assertThat(logbookCreationResult.getErrorCode()).isEqualTo(0);
        return assertDoesNotThrow(
                () -> testControllerHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        logbookCreationResult.getPayload()
                )
        );
    }
}
