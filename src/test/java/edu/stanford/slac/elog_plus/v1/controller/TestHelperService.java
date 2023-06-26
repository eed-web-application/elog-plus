package edu.stanford.slac.elog_plus.v1.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import org.assertj.core.api.AssertionsForClassTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Service()
public class TestHelperService {
    public String newAttachment(MockMvc mockMvc, MockMultipartFile file) throws Exception {
        MvcResult result_upload = mockMvc.perform(
                        multipart("/v1/attachment").file(file)
                )
                .andExpect(status().isOk())
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result_upload.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        ApiResultResponse<String> res = new ObjectMapper().readValue(result_upload.getResponse().getContentAsString(), new TypeReference<>() {
        });
        assertThat(res.getErrorCode()).isEqualTo(0);
        return res.getPayload();
    }

    public void checkDownloadedFile(MockMvc mockMvc, String attachmentID, String mediaType, Integer size) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/v1/attachment/{attachmentId}", attachmentID)
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                )
                .andExpect(status().isOk())
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        AssertionsForClassTypes.assertThat(result.getResponse().getContentAsByteArray().length).isEqualTo(size);
        AssertionsForClassTypes.assertThat(result.getResponse().getContentType()).isEqualTo(mediaType);
    }
}
