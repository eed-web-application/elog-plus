package edu.stanford.slac.elog_plus.v1.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.auth.test_mock_auth.JWTHelper;
import edu.stanford.slac.elog_plus.v1.service.SharedUtilityService;
import lombok.AllArgsConstructor;
import org.assertj.core.api.AssertionsForClassTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.elog_plus.model.Authorization.Type.Read;
import static edu.stanford.slac.elog_plus.model.Authorization.Type.Write;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Service()
public class TestControllerHelperService {
    private final AppProperties appProperties;

    public TestControllerHelperService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public  ApiResultResponse<String> getNewLogbookWithNameWithAuthorization(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String logbookName,
            List<AuthorizationDTO> authorizations) {
        var newLogbookApiResult = assertDoesNotThrow(
                () -> createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        userInfo,
                        NewLogbookDTO
                                .builder()
                                .name(logbookName)
                                .build()
                )
        );

         AssertionsForClassTypes.assertThat(newLogbookApiResult)
                 .isNotNull()
                 .extracting(
                         ApiResultResponse::getErrorCode
                 )
                 .isEqualTo(0);

        var updateApiResult = assertDoesNotThrow(
                () -> updateLogbook(
                        mockMvc,
                        status().isCreated(),
                        userInfo,
                        newLogbookApiResult.getPayload(),
                        UpdateLogbookDTO
                                .builder()
                                .name(logbookName)
                                .shifts(Collections.emptyList())
                                .tags(Collections.emptyList())
                                .authorization(
                                        authorizations
                                )
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(updateApiResult)
                .isNotNull()
                .extracting(
                        ApiResultResponse::getErrorCode
                )
                .isEqualTo(0);
        return newLogbookApiResult;
    }

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

    public ApiResultResponse<String> createNewLog(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            EntryNewDTO newLog) throws Exception {
        var postBuilder = post("/v1/entries")
                .content(
                        new ObjectMapper().writeValueAsString(
                                newLog
                        )
                )
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        userInfo.ifPresent(login -> postBuilder.header(appProperties.getUserHeaderName(), JWTHelper.generateJwt(login)));

        MvcResult result = mockMvc.perform(postBuilder)
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
            EntryImportDTO entryToImport,
            MockMultipartFile... files) throws Exception {
        MockMultipartHttpServletRequestBuilder multiPartBuilder = multipart("/v1/import");
        if (entryToImport != null) {
            MockPart p = new MockPart(
                    "entry",
                    new ObjectMapper().writeValueAsString(entryToImport).getBytes(StandardCharsets.UTF_8)
            );
            p.getHeaders().add(
                    "Content-Type",
                    MediaType.APPLICATION_JSON_VALUE
            );
            multiPartBuilder.part(
                    p
            );
        }

        for (MockMultipartFile a :
                files) {
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

    public ApiResultResponse<String> createNewSupersedeLog(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String supersededLogId,
            EntryNewDTO newLog
    ) throws Exception {
        var postBuilder = post("/v1/entries/{id}/supersede", supersededLogId)
                .content(
                        new ObjectMapper().writeValueAsString(
                                newLog
                        )
                )
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        userInfo.ifPresent(login -> postBuilder.header(appProperties.getUserHeaderName(), JWTHelper.generateJwt(login)));

        MvcResult result = mockMvc.perform(
                        postBuilder
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

    public ApiResultResponse<String> createNewFollowUpLog(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String followedLogID,
            EntryNewDTO newLog) throws Exception {
        var postBuilder = post("/v1/entries/{id}/follow-ups", followedLogID)
                .content(
                        new ObjectMapper().writeValueAsString(
                                newLog
                        )
                )
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> postBuilder.header(appProperties.getUserHeaderName(), JWTHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        postBuilder
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
        return getFullLog(mockMvc, resultMatcher, id, includeFollowUps, includeFollowingUps, includeHistory, false, false);
    }

    public ApiResultResponse<EntryDTO> getFullLog(MockMvc mockMvc, ResultMatcher resultMatcher, String id, boolean includeFollowUps, boolean includeFollowingUps, boolean includeHistory, boolean includeReferences, boolean includeReferencedBy) throws Exception {
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
        if (includeReferences) {
            request.param("includeReferences", String.valueOf(true));
        }
        if (includeReferencedBy) {
            request.param("includeReferencedBy", String.valueOf(true));
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

    public ApiResultResponse<List<EntrySummaryDTO>> getReferencesByEntryId(MockMvc mockMvc, ResultMatcher resultMatcher, String id) throws Exception {
        MockHttpServletRequestBuilder request = get("/v1/entries/{id}/references", id)
                .accept(MediaType.APPLICATION_JSON);

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
            Optional<Boolean> requireAllTags,
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
        requireAllTags.ifPresent(b -> getBuilder.param("requireAllTags", String.valueOf(b)));
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
            Optional<String> userInfo,
            NewLogbookDTO newLogbookDTO) throws Exception {

        var getBuilder = post("/v1/logbooks")
                .content(
                        new ObjectMapper().writeValueAsString(
                                newLogbookDTO
                        )
                )
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
        //apply auth
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), JWTHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        getBuilder
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
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<Boolean> includeAuthorizations,
            Optional<List<String>> filterForAuthorizationTypes
    ) throws Exception {

        MockHttpServletRequestBuilder getBuilder =
                get("/v1/logbooks")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), JWTHelper.generateJwt(login)));
        includeAuthorizations.ifPresent( b->getBuilder.param("includeAuthorizations", String.valueOf(b)));
        filterForAuthorizationTypes.ifPresent(authStr -> {
            String[] tlArray = new String[authStr.size()];
            authStr.toArray(tlArray);
            getBuilder.param("filterForAuthorizationTypes", tlArray);
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

    public ApiResultResponse<LogbookDTO> getLogbookByID(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String logbookID) throws Exception {
        MockHttpServletRequestBuilder getBuilder =
                get("/v1/logbooks/{logbookId}", logbookID)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), JWTHelper.generateJwt(login)));
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
            Optional<String> userInfo,
            String logbookId,
            NewTagDTO newTagDTO) throws Exception {
        var postBuilder = post("/v1/logbooks/{logbookId}/tags", logbookId)
                .content(
                        new ObjectMapper().writeValueAsString(
                                newTagDTO
                        )
                )
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);

        userInfo.ifPresent(login -> postBuilder.header(appProperties.getUserHeaderName(), JWTHelper.generateJwt(login)));

        MvcResult result = mockMvc.perform(
                        postBuilder
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
            Optional<String> userInfo,
            String logbookId) throws Exception {
        var getBuilder = get("/v1/logbooks/{logbookId}/tags", logbookId)
                .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), JWTHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(getBuilder)
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
            Optional<String> userInfo,
            String logbookId,
            List<ShiftDTO> replacementShiftDTO) throws Exception {
        MockHttpServletRequestBuilder putBuilder =
                put("/v1/logbooks/{logbookId}/shifts", logbookId)
                        .content(
                                new ObjectMapper().writeValueAsString(
                                        replacementShiftDTO
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);

        userInfo.ifPresent(login -> putBuilder.header(appProperties.getUserHeaderName(), JWTHelper.generateJwt(login)));

        MvcResult result = mockMvc.perform(
                        putBuilder
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
            Optional<String> userInfo,
            String logbookId,
            UpdateLogbookDTO updateLogbookDTO) throws Exception {
        MockHttpServletRequestBuilder putBuilder =
                put("/v1/logbooks/{logbookId}", logbookId)
                        .content(
                                new ObjectMapper().writeValueAsString(
                                        updateLogbookDTO
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> putBuilder.header(appProperties.getUserHeaderName(), JWTHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        putBuilder
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

    public ApiResultResponse<PersonDTO> getMe(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo) throws Exception {
        MockHttpServletRequestBuilder getBuilder =
                get("/v1/auth/me")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), JWTHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        getBuilder
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

    public ApiResultResponse<List<PersonDTO>> findUsers(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> search) throws Exception {
        MockHttpServletRequestBuilder getBuilder =
                get("/v1/auth/users")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), JWTHelper.generateJwt(login)));
        search.ifPresent(string -> getBuilder.param("search", string));
        MvcResult result = mockMvc.perform(
                        getBuilder
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

    public ApiResultResponse<List<GroupDTO>> findGroups(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> search) throws Exception {
        MockHttpServletRequestBuilder getBuilder =
                get("/v1/auth/groups")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), JWTHelper.generateJwt(login)));
        search.ifPresent(string -> getBuilder.param("search", string));
        MvcResult result = mockMvc.perform(
                        getBuilder
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
