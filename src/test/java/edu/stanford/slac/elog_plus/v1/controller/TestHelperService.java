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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Service()
public class TestHelperService {
    public ApiResultResponse<String> newAttachment(MockMvc mockMvc, ResultMatcher resultMatcher, MockMultipartFile file) throws Exception {
        MvcResult result_upload = mockMvc.perform(
                        multipart("/v1/attachment").file(file)
                )
                .andExpect(resultMatcher)
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

    public void checkDownloadedFile(MockMvc mockMvc, ResultMatcher resultMatcher, String attachmentID, String mediaType) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/v1/attachment/{id}/download", attachmentID)
                                .contentType(mediaType)
                )
                .andExpect(resultMatcher)
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        AssertionsForClassTypes.assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0);
        AssertionsForClassTypes.assertThat(result.getResponse().getContentType()).isEqualTo(mediaType);
    }

    public void checkDownloadedPreview(MockMvc mockMvc, ResultMatcher resultMatcher, String attachmentID, String mediaType) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/v1/attachment/{id}/preview.jpg", attachmentID)
                                .contentType(mediaType)
                )
                .andExpect(resultMatcher)
                .andReturn();
        Optional<ControllerLogicException> someException = Optional.ofNullable((ControllerLogicException) result.getResolvedException());
        if (someException.isPresent()) {
            throw someException.get();
        }
        AssertionsForClassTypes.assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(0);
        AssertionsForClassTypes.assertThat(result.getResponse().getContentType()).isEqualTo(mediaType);
    }

    public ApiResultResponse<String> createNewLog(MockMvc mockMvc, ResultMatcher resultMatcher, EntryNewDTO newLog) throws Exception {
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

    public ApiResultResponse<String> uploadWholeEntry(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            EntryNewDTO entryNew,
            MockMultipartFile... attachement) throws Exception {
        MockMultipartHttpServletRequestBuilder multiPartBuilder = multipart("/v1/upload");
        if (entryNew != null) {
            multiPartBuilder.param(
                    "entry",
                    new ObjectMapper().writeValueAsString(
                            entryNew
                    )
            );
        }

        for (MockMultipartFile a :
                attachement) {
            multiPartBuilder.file(a);
        }
        MvcResult result = mockMvc.perform(
                        multiPartBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        if (result.getResolvedException() != null) {
            throw result.getResolvedException();
        }
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<String> createNewSupersedeLog(MockMvc mockMvc, ResultMatcher resultMatcher, String supersededLogId, EntryNewDTO newLog) throws Exception {
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

    public ApiResultResponse<String> createNewFollowUpLog(MockMvc mockMvc, ResultMatcher resultMatcher, String followedLogID, EntryNewDTO newLog) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/v1/entries/{id}/follow-ups", followedLogID)
                                .content(
                                        new ObjectMapper().writeValueAsString(
                                                newLog
                                        )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
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

    public ApiResultResponse<List<EntrySummaryDTO>> getAllFollowUpLog(MockMvc mockMvc, ResultMatcher resultMatcher, String followedLogID) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/v1/entries/{id}/follow-ups", followedLogID)
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

    public ApiResultResponse<EntryDTO> getFullLog(MockMvc mockMvc, ResultMatcher resultMatcher, String id) throws Exception {
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

    public ApiResultResponse<EntryDTO> getFullLog(MockMvc mockMvc, ResultMatcher resultMatcher, String id, boolean includeFollowUps) throws Exception {
        return getFullLog(mockMvc, resultMatcher, id, includeFollowUps, false, false);
    }

    public ApiResultResponse<EntryDTO> getFullLog(MockMvc mockMvc, ResultMatcher resultMatcher, String id, boolean includeFollowUps, boolean includeFollowingUps, boolean includeHistory) throws Exception {
        MockHttpServletRequestBuilder request = get("/v1/entries/{id}", id)
                .accept(MediaType.APPLICATION_JSON);
        if (includeFollowUps) {
            request.param("includeFollowUps", String.valueOf(true));
        }
        if (includeFollowingUps) {
            request.param("includeFollowingUps", String.valueOf(true));
        }
        if (includeHistory) {
            request.param("includeHistory", String.valueOf(true));
        }

        MvcResult result = mockMvc.perform(
                        request
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

    public ApiResultResponse<List<EntrySummaryDTO>> submitSearchByGetWithAnchor(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> anchorId,
            Optional<LocalDateTime> startDate,
            Optional<LocalDateTime> endDate,
            Optional<Integer> contextSize,
            Optional<Integer> limit,
            Optional<String> search,
            Optional<List<String>> tags,
            Optional<List<String>> logBook,
            Optional<Boolean> sortByLogDate) throws Exception {

        MockHttpServletRequestBuilder getBuilder =
                get("/v1/entries")
                        .accept(MediaType.APPLICATION_JSON);
        anchorId.ifPresent(string -> getBuilder.param("anchorId", string));
        startDate.ifPresent(localDateTime -> getBuilder.param("startDate", String.valueOf(localDateTime)));
        endDate.ifPresent(localDateTime -> getBuilder.param("endDate", String.valueOf(localDateTime)));
        contextSize.ifPresent(size -> getBuilder.param("contextSize", String.valueOf(size)));
        limit.ifPresent(size -> getBuilder.param("limit", String.valueOf(size)));
        search.ifPresent(text -> getBuilder.param("search", text));
        sortByLogDate.ifPresent(b -> getBuilder.param("sortByLogDate", String.valueOf(b)));
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
                .andExpect(resultMatcher)
                .andReturn();
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<String> createNewLogbook(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            NewLogbookDTO newLogbookDTO) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/v1/logbooks")
                                .content(
                                        new ObjectMapper().writeValueAsString(
                                                newLogbookDTO
                                        )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
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

    public ApiResultResponse<List<LogbookDTO>> getAllLogbook(
            MockMvc mockMvc,
            ResultMatcher resultMatcher) throws Exception {

        MockHttpServletRequestBuilder getBuilder =
                get("/v1/logbooks")
                        .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(
                        getBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<LogbookDTO> getLogbookByID(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            String logbookID) throws Exception {

        MockHttpServletRequestBuilder getBuilder =
                get("/v1/logbooks/{logbookId}", logbookID)
                        .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(
                        getBuilder
                )
                .andExpect(resultMatcher)
                .andReturn();
        return new ObjectMapper().readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    public ApiResultResponse<String> createNewLogbookTags(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            String logbookId,
            NewTagDTO newTagDTO) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/v1/logbooks/{logbookId}/tags", logbookId)
                                .content(
                                        new ObjectMapper().writeValueAsString(
                                                newTagDTO
                                        )
                                )
                                .contentType(MediaType.APPLICATION_JSON)
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

    public ApiResultResponse<List<TagDTO>> getLogbookTags(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            String logbookId) throws Exception {
        MvcResult result = mockMvc.perform(
                        get("/v1/logbooks/{logbookId}/tags", logbookId)
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

    public ApiResultResponse<List<TagDTO>> getLogbookTagsFromTagsController(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<List<String>> logbooksName) throws Exception {
        MockHttpServletRequestBuilder getRequest = get("/v1/tags")
                .accept(MediaType.APPLICATION_JSON);
        logbooksName.ifPresent(lb -> {
            String[] lbArray = new String[lb.size()];
            lb.toArray(lbArray);
            getRequest.param("logbooks", lbArray);
        });

        MvcResult result = mockMvc.perform(
                        getRequest
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

    public ApiResultResponse<Boolean> replaceShiftForLogbook(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            String logbookId,
            List<ShiftDTO> replacementShiftDTO) throws Exception {
        MockHttpServletRequestBuilder putRequest =
                put("/v1/logbooks/{logbookId}/shifts", logbookId)
                        .content(
                                new ObjectMapper().writeValueAsString(
                                        replacementShiftDTO
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(
                        putRequest
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

    public ApiResultResponse<Boolean> updateLogbook(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            String logbookId,
            UpdateLogbookDTO updateLogbookDTO) throws Exception {
        MockHttpServletRequestBuilder putRequest =
                put("/v1/logbooks/{logbookId}", logbookId)
                        .content(
                                new ObjectMapper().writeValueAsString(
                                        updateLogbookDTO
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(
                        putRequest
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

    public ApiResultResponse<String> findSummaryIdByShiftNameAndDate(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            String shiftId,
            LocalDate date
    ) throws Exception {
        MockHttpServletRequestBuilder putRequest =
                get(
                        "/v1/entries/{shiftId}/summaries/{date}",
                        shiftId,
                        date
                )
                        .accept(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(
                        putRequest
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
}
