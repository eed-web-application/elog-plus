package edu.stanford.slac.elog_plus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.eed_java_backend_example.api.v1.dto.ApiResultResponse;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class HelloWorldControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void fetchQueryParameter() throws Exception {
        MvcResult get_result = mockMvc.perform(
                        get("/v1/hello/world")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();
        ApiResultResponse<String> api_result = new ObjectMapper().readValue(
                get_result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        AssertionsForClassTypes.assertThat(api_result).isNotNull();
        AssertionsForClassTypes.assertThat(api_result.getErrorCode()).isEqualTo(0);
        AssertionsForClassTypes.assertThat(api_result.getPayload())
                .isEqualTo("hello world");
    }
}
