package edu.stanford.slac.elog_plus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class QueryController {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private QueryParameterConfigurationDTO queryParameterConfigurationDTO;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Log.class);
    }

    @Test
    public void fetchQueryParameter() throws Exception {
        var queryParameter = getQueryParameter();
        AssertionsForClassTypes.assertThat(queryParameter).isNotNull();
        AssertionsForClassTypes.assertThat(queryParameter.getErrorCode()).isEqualTo(0);
        AssertionsForClassTypes.assertThat(queryParameter.getPayload()).isNotNull();
        AssertionsForClassTypes.assertThat(queryParameter.getPayload().logbook().size())
                .isEqualTo(
                        queryParameterConfigurationDTO.logbook().size()
                );
    }

    @Test
    public void emptyParameterQuery() throws Exception {
        var queryResult = submitSearchByPost(QueryParameterDTO
                .builder()
                .build()
        );
        AssertionsForClassTypes.assertThat(queryResult).isNotNull();
        AssertionsForClassTypes.assertThat(queryResult.getErrorCode()).isEqualTo(0);
        AssertionsForClassTypes.assertThat(queryResult.getPayload()).isNotNull();
    }

    @Test
    public void searchByGetCheckPagingWithEmptyResult() throws Exception {
        var queryResult = submitSearchByGet(1, 5, Collections.emptyList());
        AssertionsForClassTypes.assertThat(queryResult).isNotNull();
        AssertionsForClassTypes.assertThat(queryResult.getErrorCode()).isEqualTo(0);
        AssertionsForClassTypes.assertThat(queryResult.getPayload()).isNotNull();
        AssertionsForClassTypes.assertThat(queryResult.getPayload().getContent().size()).isEqualTo(0);
    }

    @Test
    public void searchByGetAndLogbookCheckPagingWithEmptyResult() throws Exception {
        var queryResult = submitSearchByGet(1, 5, List.of("MCC"));
        AssertionsForClassTypes.assertThat(queryResult).isNotNull();
        AssertionsForClassTypes.assertThat(queryResult.getErrorCode()).isEqualTo(0);
        AssertionsForClassTypes.assertThat(queryResult.getPayload()).isNotNull();
        AssertionsForClassTypes.assertThat(queryResult.getPayload().getContent().size()).isEqualTo(0);
    }

    private ApiResultResponse<QueryPagedResultDTO<LogDTO>> submitSearchByPost(QueryParameterDTO queryParameter) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/v1/logs")
                                .content(
                                        new ObjectMapper().writeValueAsString(
                                                queryParameter
                                        )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();

        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    private ApiResultResponse<QueryPagedResultDTO<LogDTO>> submitSearchByGet(
            int page,
            int size,
            List<String> logbook) throws Exception {
        MvcResult result;
        if(logbook.size()==0) {
            result = mockMvc.perform(
                            get("/v1/logs")
                                    .param("page", String.valueOf(page))
                                    .param("size", String.valueOf(size))
                                    .param("logbook", "")
                                    .accept(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andReturn();
        } else {
            String[] lbArray = new String[logbook.size()];
            logbook.toArray(lbArray);
            result = mockMvc.perform(
                            get("/v1/logs")
                                    .param("page", String.valueOf(page))
                                    .param("size", String.valueOf(size))
                                    .param("logbook", lbArray)
                                    .accept(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andReturn();
        }

        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    private ApiResultResponse<QueryParameterConfigurationDTO> getQueryParameter() throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/v1/logbooks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();

        ApiResultResponse<QueryParameterConfigurationDTO> queryParameterConfiguration = new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        return queryParameterConfiguration;
    }
}
