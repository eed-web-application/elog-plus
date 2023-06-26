package edu.stanford.slac.elog_plus.v1.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.QueryParameterConfigurationDTO;
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
    private QueryParameterConfigurationDTO queryParameterConfigurationDTO;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Log.class);
    }
    @Test
    public void fetchLogbooks() throws Exception {
        var queryParameter = getLogbooks();
        AssertionsForClassTypes.assertThat(queryParameter).isNotNull();
        AssertionsForClassTypes.assertThat(queryParameter.getErrorCode()).isEqualTo(0);
        AssertionsForClassTypes.assertThat(queryParameter.getPayload()).isNotNull();
        AssertionsForClassTypes.assertThat(queryParameter.getPayload().logbook().size())
                .isEqualTo(
                        queryParameterConfigurationDTO.logbook().size()
                );
    }

    private ApiResultResponse<QueryParameterConfigurationDTO> getLogbooks() throws Exception {
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