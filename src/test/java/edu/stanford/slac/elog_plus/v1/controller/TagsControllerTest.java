package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.NewLogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewTagDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.TagDTO;
import edu.stanford.slac.elog_plus.model.Logbook;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class TagsControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestHelperService testHelperService;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Logbook.class);
    }

    @Test
    public void testGetOneLogbookTags() {
        ApiResultResponse<String> newLogbookResult = assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO
                                .builder()
                                .name("Logbook1")
                                .build()
                )
        );
        assertThat(newLogbookResult).isNotNull();
        assertThat(newLogbookResult.getErrorCode()).isEqualTo(0);

        for(int idx = 0; idx <= 99; idx++) {
            int finalIdx = idx;
            ApiResultResponse<String> newTagResult =  assertDoesNotThrow(
                    () -> testHelperService.createNewLogbookTags(
                            mockMvc,
                            status().isCreated(),
                            newLogbookResult.getPayload(),
                            NewTagDTO
                                    .builder()
                                    .name(String.format("Tag-%d", finalIdx))
                                    .build()
                    )
            );
            assertThat(newTagResult).isNotNull();
            assertThat(newTagResult.getErrorCode()).isEqualTo(0);
        }

        ApiResultResponse<List<TagDTO>> allTagsResult = assertDoesNotThrow(
                () -> testHelperService.getLogbookTagsFromTagsController(
                        mockMvc,
                        status().isOk(),
                        Optional.empty()
                )
        );
        assertThat(allTagsResult).isNotNull();
        assertThat(allTagsResult.getErrorCode()).isEqualTo(0);
        assertThat(allTagsResult.getPayload().size()).isEqualTo(100);
    }

    @Test
    public void testGetFromAllLogbookTags() {
        //add first logbook
        ApiResultResponse<String> newLogbookResult = assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO
                                .builder()
                                .name("Logbook1")
                                .build()
                )
        );
        assertThat(newLogbookResult).isNotNull();
        assertThat(newLogbookResult.getErrorCode()).isEqualTo(0);

        for(int idx = 0; idx <= 99; idx++) {
            int finalIdx = idx;
            ApiResultResponse<String> newTagResult =  assertDoesNotThrow(
                    () -> testHelperService.createNewLogbookTags(
                            mockMvc,
                            status().isCreated(),
                            newLogbookResult.getPayload(),
                            NewTagDTO
                                    .builder()
                                    .name(String.format("Tag-%d", finalIdx))
                                    .build()
                    )
            );
            assertThat(newTagResult).isNotNull();
            assertThat(newTagResult.getErrorCode()).isEqualTo(0);
        }

        //add second logbook
        ApiResultResponse<String> newLogbookResultTwo = assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO
                                .builder()
                                .name("Logbook2")
                                .build()
                )
        );
        assertThat(newLogbookResultTwo).isNotNull();
        assertThat(newLogbookResultTwo.getErrorCode()).isEqualTo(0);

        for(int idx = 50; idx <= 149; idx++) {
            int finalIdx = idx;
            ApiResultResponse<String> newTagResult =  assertDoesNotThrow(
                    () -> testHelperService.createNewLogbookTags(
                            mockMvc,
                            status().isCreated(),
                            newLogbookResultTwo.getPayload(),
                            NewTagDTO
                                    .builder()
                                    .name(String.format("Tag-%d", finalIdx))
                                    .build()
                    )
            );
            assertThat(newTagResult).isNotNull();
            assertThat(newTagResult.getErrorCode()).isEqualTo(0);
        }

        ApiResultResponse<List<TagDTO>> allTagsResult = assertDoesNotThrow(
                () -> testHelperService.getLogbookTagsFromTagsController(
                        mockMvc,
                        status().isOk(),
                        Optional.empty()
                )
        );
        assertThat(allTagsResult).isNotNull();
        assertThat(allTagsResult.getErrorCode()).isEqualTo(0);
        assertThat(allTagsResult.getPayload().size()).isEqualTo(200);
    }
}
