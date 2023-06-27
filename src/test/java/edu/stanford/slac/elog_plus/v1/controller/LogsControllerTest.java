package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.NewLogDTO;
import edu.stanford.slac.elog_plus.model.Log;
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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class LogsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TestHelperService testHelperService;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Log.class);
    }

    @Test
    public void searchByGetCheckPagingWithEmptyResult() throws Exception {
        var queryResult = testHelperService.submitSearchByGet(mockMvc,1, 5, Collections.emptyList());
        AssertionsForClassTypes.assertThat(queryResult).isNotNull();
        AssertionsForClassTypes.assertThat(queryResult.getErrorCode()).isEqualTo(0);
        AssertionsForClassTypes.assertThat(queryResult.getPayload()).isNotNull();
        AssertionsForClassTypes.assertThat(queryResult.getPayload().getContent().size()).isEqualTo(0);
    }

    @Test
    public void searchByGetAndLogbookCheckPagingWithEmptyResult() throws Exception {
        var queryResult = testHelperService.submitSearchByGet(mockMvc,1, 5, List.of("MCC"));
        AssertionsForClassTypes.assertThat(queryResult).isNotNull();
        AssertionsForClassTypes.assertThat(queryResult.getErrorCode()).isEqualTo(0);
        AssertionsForClassTypes.assertThat(queryResult.getPayload()).isNotNull();
        AssertionsForClassTypes.assertThat(queryResult.getPayload().getContent().size()).isEqualTo(0);
    }

    @Test
    public void createNewLog() throws Exception {
        ApiResultResponse<String> newLogID =
                assertDoesNotThrow(
                        () ->
                                testHelperService.createNewLog(
                                        mockMvc,
                                        NewLogDTO
                                                .builder()
                                                .logbook("MCC")
                                                .text("This is a log for test")
                                                .title("A very wonderful log")
                                                .build()
                                )
                );

        AssertionsForClassTypes.assertThat(newLogID).isNotNull();
        AssertionsForClassTypes.assertThat(newLogID.getErrorCode()).isEqualTo(0);

        var queryResult = testHelperService.submitSearchByGet(mockMvc, 0, 5, List.of("MCC"));
        AssertionsForClassTypes.assertThat(queryResult.getPayload().getContent().size()).isEqualTo(1);
    }
}
