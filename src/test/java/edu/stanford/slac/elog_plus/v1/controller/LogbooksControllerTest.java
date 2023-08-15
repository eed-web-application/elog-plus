package edu.stanford.slac.elog_plus.v1.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.LogbookAlreadyExists;
import edu.stanford.slac.elog_plus.exception.TagAlreadyExists;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.LogbookService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class LogbooksControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestHelperService testHelperService;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Logbook.class);
        mongoTemplate.remove(new Query(), Entry.class);
    }

    @Test
    public void createNewLogbookAndGet() throws Exception {
        ApiResultResponse<String> creationResult = assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO.builder()
                                .name("new-logbooks")
                                .build()
                )
        );

        assertThat(creationResult).isNotNull();
        assertThat(creationResult.getErrorCode()).isEqualTo(0);
        assertThat(creationResult.getPayload()).isNotEmpty();

        ApiResultResponse<LogbookDTO> getLogResult = assertDoesNotThrow(
                () -> testHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        creationResult.getPayload()
                )
        );
        assertThat(getLogResult).isNotNull();
        assertThat(getLogResult.getPayload().id()).isEqualTo(creationResult.getPayload());
    }


    @Test
    public void failCreatingTwoSameLogbook() {
        ApiResultResponse<String> creationResult = assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO.builder()
                                .name("new-logbooks")
                                .build()
                )
        );
        assertThat(creationResult).isNotNull();
        assertThat(creationResult.getErrorCode()).isEqualTo(0);
        assertThat(creationResult.getPayload()).isNotEmpty();

        LogbookAlreadyExists alreadyExistsException = assertThrows(
                LogbookAlreadyExists.class,
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isConflict(),
                        NewLogbookDTO.builder()
                                .name("New Logbooks")
                                .build()
                )
        );
        assertThat(alreadyExistsException.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void createAndGetLogbookTags() throws Exception {
        ApiResultResponse<String> creationResult = assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO.builder()
                                .name("new-logbooks")
                                .build()
                )
        );

        assertThat(creationResult).isNotNull();
        assertThat(creationResult.getErrorCode()).isEqualTo(0);
        assertThat(creationResult.getPayload()).isNotEmpty();

        ApiResultResponse<String> newTagResult = assertDoesNotThrow(
                () -> testHelperService.createNewLogbookTags(
                        mockMvc,
                        status().isCreated(),
                        creationResult.getPayload(),
                        NewTagDTO
                                .builder()
                                .name("new-tag")
                                .build()
                )
        );
        assertThat(newTagResult).isNotNull();
        assertThat(newTagResult.getErrorCode()).isEqualTo(0);
        assertThat(newTagResult.getPayload()).isNotEmpty();

        ApiResultResponse<List<TagDTO>> allTagResult = assertDoesNotThrow(
                () -> testHelperService.getLogbookTags(
                        mockMvc,
                        status().isOk(),
                        creationResult.getPayload()
                )
        );

        assertThat(allTagResult).isNotNull();
        assertThat(allTagResult.getErrorCode()).isEqualTo(0);
        assertThat(allTagResult.getPayload()).hasSize(1);
        assertThat(allTagResult.getPayload()).extracting("name").contains("new-tag");
    }


    @Test
    public void failCreatingTwoSameTagsOnTheSameLogbook() {
        ApiResultResponse<String> creationResult = assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO.builder()
                                .name("new-logbooks")
                                .build()
                )
        );
        assertThat(creationResult).isNotNull();
        assertThat(creationResult.getErrorCode()).isEqualTo(0);
        assertThat(creationResult.getPayload()).isNotEmpty();

        ApiResultResponse<String> newTagResult = assertDoesNotThrow(
                () -> testHelperService.createNewLogbookTags(
                        mockMvc,
                        status().isCreated(),
                        creationResult.getPayload(),
                        NewTagDTO
                                .builder()
                                .name("new-tag")
                                .build()
                )
        );
        assertThat(newTagResult).isNotNull();
        assertThat(newTagResult.getErrorCode()).isEqualTo(0);
        assertThat(newTagResult.getPayload()).isNotEmpty();

        TagAlreadyExists tagAlreadyExistsException = assertThrows(
                TagAlreadyExists.class,
                () -> testHelperService.createNewLogbookTags(
                        mockMvc,
                        status().isConflict(),
                        creationResult.getPayload(),
                        NewTagDTO
                                .builder()
                                .name("New Tag")
                                .build()
                )
        );
        assertThat(tagAlreadyExistsException.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void createAndGetShifts() throws Exception {
        ApiResultResponse<String> creationResult = assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO.builder()
                                .name("new-logbooks")
                                .build()
                )
        );

        assertThat(creationResult).isNotNull();
        assertThat(creationResult.getErrorCode()).isEqualTo(0);
        assertThat(creationResult.getPayload()).isNotEmpty();

        ApiResultResponse<Boolean> replacementResult = assertDoesNotThrow(
                () -> testHelperService.replaceShiftForLogbook(
                        mockMvc,
                        status().isCreated(),
                        creationResult.getPayload(),
                        List.of(
                                ShiftDTO.builder()
                                        .name("Shift A")
                                        .from("08:00")
                                        .to("10:59")
                                        .build()
                        )
                )
        );
        assertThat(replacementResult).isNotNull();
        assertThat(replacementResult.getErrorCode()).isEqualTo(0);

        ApiResultResponse<LogbookDTO> logbookResult = assertDoesNotThrow(
                () -> testHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        creationResult.getPayload()
                )
        );

        assertThat(logbookResult).isNotNull();
        assertThat(logbookResult.getErrorCode()).isEqualTo(0);
        assertThat(logbookResult.getPayload().shifts()).extracting("name").contains("Shift A");

        // replace and remove the old one

        replacementResult = assertDoesNotThrow(
                () -> testHelperService.replaceShiftForLogbook(
                        mockMvc,
                        status().isCreated(),
                        creationResult.getPayload(),
                        List.of(
                                ShiftDTO.builder()
                                        .name("Shift B")
                                        .from("08:00")
                                        .to("10:59")
                                        .build()
                        )
                )
        );
        assertThat(replacementResult).isNotNull();
        assertThat(replacementResult.getErrorCode()).isEqualTo(0);

        //check
        logbookResult = assertDoesNotThrow(
                () -> testHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        creationResult.getPayload()
                )
        );

        assertThat(logbookResult).isNotNull();
        assertThat(logbookResult.getErrorCode()).isEqualTo(0);
        assertThat(logbookResult.getPayload().shifts()).extracting("name").contains("Shift B");

        // update Shift B and create new one
        ApiResultResponse<LogbookDTO> finalLogbookResult = logbookResult;
        replacementResult = assertDoesNotThrow(
                () -> testHelperService.replaceShiftForLogbook(
                        mockMvc,
                        status().isCreated(),
                        creationResult.getPayload(),
                        List.of(
                                finalLogbookResult.getPayload().shifts().get(0).toBuilder()
                                        .from("07:00")
                                        .to("07:59")
                                        .build(),
                                ShiftDTO.builder()
                                        .name("New After B")
                                        .from("08:00")
                                        .to("10:59")
                                        .build()
                        )
                )
        );
        assertThat(replacementResult).isNotNull();
        assertThat(replacementResult.getErrorCode()).isEqualTo(0);

        logbookResult = assertDoesNotThrow(
                () -> testHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        creationResult.getPayload()
                )
        );

        assertThat(logbookResult).isNotNull();
        assertThat(logbookResult.getErrorCode()).isEqualTo(0);
        assertThat(logbookResult.getPayload().shifts()).extracting("from").contains("07:00", "08:00");
    }

    @Test
    public void updateLogbook() throws Exception {
        ApiResultResponse<String> creationResult = assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO.builder()
                                .name("new-logbooks")
                                .build()
                )
        );

        assertThat(creationResult).isNotNull();
        assertThat(creationResult.getErrorCode()).isEqualTo(0);
        assertThat(creationResult.getPayload()).isNotEmpty();

        ApiResultResponse<Boolean> replacementResult = assertDoesNotThrow(
                () -> testHelperService.updateLogbook(
                        mockMvc,
                        status().isCreated(),
                        creationResult.getPayload(),
                        UpdateLogbookDTO
                                .builder()
                                .name("updated name")
                                .tags(
                                        List.of(
                                                TagDTO.builder()
                                                        .name("tag-1")
                                                        .build()
                                        )
                                )
                                .shifts(
                                        List.of(
                                                ShiftDTO.builder()
                                                        .name("Shift A")
                                                        .from("08:00")
                                                        .to("10:59")
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(replacementResult).isNotNull();
        assertThat(replacementResult.getErrorCode()).isEqualTo(0);

        ApiResultResponse<LogbookDTO> fullLogbook = assertDoesNotThrow(
                () -> testHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        creationResult.getPayload()
                )
        );
        assertThat(fullLogbook.getErrorCode()).isEqualTo(0);
        assertThat(fullLogbook.getPayload().name()).isEqualTo("updated-name");
        assertThat(fullLogbook.getPayload().tags()).hasSize(1).extracting("name").contains("tag-1");
        assertThat(fullLogbook.getPayload().shifts()).hasSize(1).extracting("name").contains("Shift A");
    }

    @Test
    public void updateLogbookOnlyShifts() throws Exception {
        ApiResultResponse<String> creationResult = assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO.builder()
                                .name("new-logbooks")
                                .build()
                )
        );

        assertThat(creationResult).isNotNull();
        assertThat(creationResult.getErrorCode()).isEqualTo(0);
        assertThat(creationResult.getPayload()).isNotEmpty();

        ApiResultResponse<Boolean> replacementResult = assertDoesNotThrow(
                () -> testHelperService.updateLogbook(
                        mockMvc,
                        status().isCreated(),
                        creationResult.getPayload(),
                        UpdateLogbookDTO
                                .builder()
                                .name("updated name")
                                .tags(
                                        Collections.emptyList()
                                )
                                .shifts(
                                        List.of(
                                                ShiftDTO.builder()
                                                        .id(null)
                                                        .name("Morning shift")
                                                        .from("16:09")
                                                        .to("17:09")
                                                        .build()
                                        )
                                )
                                .build()
                )
        );
        assertThat(replacementResult).isNotNull();
        assertThat(replacementResult.getErrorCode()).isEqualTo(0);

        ApiResultResponse<LogbookDTO> fullLogbook = assertDoesNotThrow(
                () -> testHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        creationResult.getPayload()
                )
        );
        assertThat(fullLogbook.getErrorCode()).isEqualTo(0);
        assertThat(fullLogbook.getPayload().name()).isEqualTo("updated-name");
        assertThat(fullLogbook.getPayload().shifts()).hasSize(1).extracting("name").contains("Morning shift");
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
        //add first logbooks
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

        //add second logbooks
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