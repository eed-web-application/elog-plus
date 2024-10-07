package edu.stanford.slac.elog_plus.v1.controller;

import com.github.javafaker.Faker;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.EntryDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.EntryImportDTO;
import edu.stanford.slac.elog_plus.api.v2.dto.ImportEntryDTO;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.LogbookService;
import edu.stanford.slac.elog_plus.service.DocumentGenerationService;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ImportV2ControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LogbookService logbookService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TestControllerHelperService testControllerHelperService;

    @Autowired
    private DocumentGenerationService documentGenerationService;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private AuthService authService;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Entry.class);
        mongoTemplate.remove(new Query(), Attachment.class);
        mongoTemplate.remove(new Query(), Logbook.class);
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
    }

    @Test
    public void uploadEntryWithNoAttachment() {
        Faker faker = new Faker();
        ImportEntryDTO dto = ImportEntryDTO
                .builder()
                // authorized user 2 to read on created logbook
                .readerUserIds(List.of("user2@slac.stanford.edu"))
                .entry(
                        EntryImportDTO
                                .builder()
                                .logbooks(List.of("new-logbook"))
                                .title(faker.book().title())
                                .text(faker.lorem().paragraph())
                                .build()
                )
                .build();

        ApiResultResponse<String> uploadResult = assertDoesNotThrow(
                () -> testControllerHelperService.importEntryV2(
                        mockMvc,
                        status().isCreated(),
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        dto,
                        new MockMultipartFile[]{})
        );

        assertThat(uploadResult.getErrorCode()).isEqualTo(0);
        assertThat(uploadResult.getPayload()).isNotEmpty();

        ApiResultResponse<EntryDTO> fullLog = assertDoesNotThrow(
                () -> testControllerHelperService.getFullLog(
                        mockMvc,
                        status().isOk(),
                        Optional.of(
                                "user2@slac.stanford.edu"
                        ),
                        uploadResult.getPayload()
                )
        );
        assertThat(fullLog.getPayload().id()).isEqualTo(uploadResult.getPayload());
    }

    @Test
    public void importEntryWithAttachment() {
        ApiResultResponse<String> uploadResult = null;
        ImportEntryDTO dto = ImportEntryDTO
                .builder()
                // authorized user 2 to read on created logbook
                .readerUserIds(List.of("user2@slac.stanford.edu"))
                .entry(
                        EntryImportDTO
                                .builder()
                                .originId("origin-id")
                                .title("Sample Title")
                                .text("sample text")
                                .logbooks(List.of("new-logbook"))
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
                                .build()
                )
                .build();

        try {
            InputStream isPng = documentGenerationService.getTestPng();
            InputStream isJpg = documentGenerationService.getTestJpeg();
            uploadResult = assertDoesNotThrow(
                    () -> testControllerHelperService.importEntryV2(
                            mockMvc,
                            status().isCreated(),
                            Optional.of(
                                    "user1@slac.stanford.edu"
                            ),
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
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        ApiResultResponse<String> finalUploadResult = uploadResult;
        ApiResultResponse<EntryDTO> fullLog = assertDoesNotThrow(
                () -> testControllerHelperService.getFullLog(
                        mockMvc,
                        status().isOk(),
                        Optional.of(
                                "user2@slac.stanford.edu"
                        ),
                        finalUploadResult.getPayload()
                )
        );
        assertThat(fullLog.getPayload().tags()).extracting("name").contains("tag-one");
    }
}
