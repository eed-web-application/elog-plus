package edu.stanford.slac.elog_plus.v1.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import org.assertj.core.api.AssertionsForClassTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Service()
public class TestHelperService {
    public ApiResultResponse<String> newAttachment(MockMvc mockMvc, MockMultipartFile file) throws Exception {
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
        return res;
    }

    public void checkDownloadedFile(MockMvc mockMvc, String attachmentID, String mediaType) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/v1/attachment/{id}/download", attachmentID)
                                .contentType(mediaType)
                )
                .andExpect(status().isOk())
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        AssertionsForClassTypes.assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0);
        AssertionsForClassTypes.assertThat(result.getResponse().getContentType()).isEqualTo(mediaType);
    }

    public void checkDownloadedPreview(MockMvc mockMvc, String attachmentID, String mediaType) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/v1/attachment/{id}/preview.jpg", attachmentID)
                                .contentType(mediaType)
                )
                .andExpect(status().isOk())
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        AssertionsForClassTypes.assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0);
        AssertionsForClassTypes.assertThat(result.getResponse().getContentType()).isEqualTo(mediaType);
    }

    public ApiResultResponse<String> createNewLog(MockMvc mockMvc, NewLogDTO newLog) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/v1/logs")
                                .content(
                                        new ObjectMapper().writeValueAsString(
                                                newLog
                                        )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<String> createNewSupersedeLog(MockMvc mockMvc, String supersededLogId, NewLogDTO newLog) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/v1/logs/{id}/supersede", supersededLogId)
                                .content(
                                        new ObjectMapper().writeValueAsString(
                                                newLog
                                        )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<String> createNewFollowUpLog(MockMvc mockMvc, String followedLogID, NewLogDTO newLog) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/v1/logs/{id}/follow-up", followedLogID)
                                .content(
                                        new ObjectMapper().writeValueAsString(
                                                newLog
                                        )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<List<SearchResultLogDTO>> getAllFollowUpLog(MockMvc mockMvc, String followedLogID) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/v1/logs/{id}/follow-up", followedLogID)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<LogDTO> getFullLog(MockMvc mockMvc, String id) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/v1/logs/{id}", id)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }


    public ApiResultResponse<LogDTO> getFullLog(MockMvc mockMvc, String id, boolean includeFollowUps) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/v1/logs/{id}", id)
                                .param("includeFollowUps", String.valueOf(includeFollowUps))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<QueryPagedResultDTO<LogDTO>> submitSearchByGet(
            MockMvc mockMvc,
            int page,
            int size,
            List<String> logbook) throws Exception {
        MvcResult result;
        if (logbook.size() == 0) {
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

}
