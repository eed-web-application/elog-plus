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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
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

    public ApiResultResponse<String> createNewLog(MockMvc mockMvc, EntryNewDTO newLog) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/v1/entries")
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

    public ApiResultResponse<String> createNewSupersedeLog(MockMvc mockMvc, String supersededLogId, EntryNewDTO newLog) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/v1/entries/{id}/supersede", supersededLogId)
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

    public ApiResultResponse<String> createNewFollowUpLog(MockMvc mockMvc, String followedLogID, EntryNewDTO newLog) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/v1/entries/{id}/follow-up", followedLogID)
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

    public ApiResultResponse<List<EntrySummaryDTO>> getAllFollowUpLog(MockMvc mockMvc, String followedLogID) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/v1/entries/{id}/follow-up", followedLogID)
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

    public ApiResultResponse<EntryDTO> getFullLog(MockMvc mockMvc, String id) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/v1/entries/{id}", id)
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

    public ApiResultResponse<EntryDTO> getFullLog(MockMvc mockMvc, String id, ResultMatcher resultMatcher) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/v1/entries/{id}", id)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(resultMatcher)
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

    public ApiResultResponse<EntryDTO> getFullLog(MockMvc mockMvc, String id, boolean includeFollowUps) throws Exception {
        return getFullLog(mockMvc, id, includeFollowUps, false, false);
    }
    public ApiResultResponse<EntryDTO> getFullLog(MockMvc mockMvc, String id, boolean includeFollowUps, boolean includeFollowingUps, boolean includeHistory) throws Exception {
        MockHttpServletRequestBuilder request = get("/v1/entries/{id}", id)
                .accept(MediaType.APPLICATION_JSON);
        if(includeFollowUps) {
            request.param("includeFollowUps", String.valueOf(true));
        }
        if(includeFollowingUps) {
            request.param("includeFollowingUps", String.valueOf(true));
        }
        if(includeHistory) {
            request.param("includeHistory", String.valueOf(true));
        }

        MvcResult result = mockMvc.perform(
                        request
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

    public ApiResultResponse<List<EntrySummaryDTO>> submitSearchByGetWithAnchor(
            MockMvc mockMvc,
            Optional<LocalDateTime> startDate,
            Optional<Integer> contextSize,
            Optional<Integer> limit,
            Optional<String> search,
            Optional<List<String>> tags,
            Optional<List<String>> logBook) throws Exception {

        MockHttpServletRequestBuilder getBuilder =
                get("/v1/entries")
                        .accept(MediaType.APPLICATION_JSON);
        startDate.ifPresent(localDateTime -> getBuilder.param("startDate", String.valueOf(localDateTime)));
        //endDate.ifPresent(localDateTime -> getBuilder.param("endDate", String.valueOf(localDateTime)));
        contextSize.ifPresent(size -> getBuilder.param("contextSize", String.valueOf(size)));
        limit.ifPresent(size -> getBuilder.param("limit", String.valueOf(size)));
        search.ifPresent(text -> getBuilder.param("search", text));
        tags.ifPresent(tl -> {
            String[] tlArray = new String[tl.size()];
            tl.toArray(tlArray);
            getBuilder.param("tags", tlArray);
        });
        logBook.ifPresent(logbook -> {
            String[] lbArray = new String[logbook.size()];
            logbook.toArray(lbArray);
            getBuilder.param("logbooks", lbArray);
        });
        MvcResult result = mockMvc.perform(
                        getBuilder
                )
                .andExpect(status().isOk())
                .andReturn();
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<String> createNewTag(MockMvc mockMvc, NewTagDTO newTagDTO) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/v1/tags")
                                .content(
                                        new ObjectMapper().writeValueAsString(
                                                newTagDTO
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

    public ApiResultResponse<List<TagDTO>> getAllTags(
            MockMvc mockMvc) throws Exception {

        MockHttpServletRequestBuilder getBuilder =
                get("/v1/tags")
                        .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(
                        getBuilder
                )
                .andExpect(status().isOk())
                .andReturn();
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }
}
