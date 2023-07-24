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

import java.util.List;

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
        ApiResultResponse<String> creationResult =  assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO.builder()
                                .name("new-logbook")
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
        ApiResultResponse<String> creationResult =  assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO.builder()
                                .name("new-logbook")
                                .build()
                )
        );
        assertThat(creationResult).isNotNull();
        assertThat(creationResult.getErrorCode()).isEqualTo(0);
        assertThat(creationResult.getPayload()).isNotEmpty();

        LogbookAlreadyExists alreadyExistsException =  assertThrows(
                LogbookAlreadyExists.class,
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isConflict(),
                        NewLogbookDTO.builder()
                                .name("New Logbook")
                                .build()
                )
        );
        assertThat(alreadyExistsException.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void createAndGetLogbookTags() throws Exception {
        ApiResultResponse<String> creationResult =  assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO.builder()
                                .name("new-logbook")
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
        ApiResultResponse<String> creationResult =  assertDoesNotThrow(
                () -> testHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        NewLogbookDTO.builder()
                                .name("new-logbook")
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
}