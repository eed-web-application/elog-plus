package edu.stanford.slac.elog_plus.v1.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.*;
import edu.stanford.slac.ad.eed.baselib.auth.JWTHelper;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.api.v2.dto.ImportEntryDTO;
import edu.stanford.slac.elog_plus.service.LogbookService;
import edu.stanford.slac.elog_plus.v1.service.SharedUtilityService;
import org.assertj.core.api.Assertions;
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
import java.util.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Service()
public class TestControllerHelperService {
    private final JWTHelper jwtHelper;
    private final AppProperties appProperties;
    private final LogbookService logbookService;
    private final SharedUtilityService sharedUtilityService;
    private final ObjectMapper objectMapper;

    public TestControllerHelperService(JWTHelper jwtHelper, AppProperties appProperties, LogbookService logbookService, SharedUtilityService sharedUtilityService, ObjectMapper objectMapper) {
        this.jwtHelper = jwtHelper;
        this.appProperties = appProperties;
        this.logbookService = logbookService;
        this.sharedUtilityService = sharedUtilityService;
        this.objectMapper = objectMapper;
    }

    public ApiResultResponse<String> getNewLogbookWithNameWithAuthorization(
            MockMvc mockMvc,
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
                        status().isOk(),
                        userInfo,
                        newLogbookApiResult.getPayload(),
                        UpdateLogbookDTO
                                .builder()
                                .name(logbookName)
                                .shifts(Collections.emptyList())
                                .tags(Collections.emptyList())
                                .authorizations(
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

    public String getTokenEmailForApplicationToken(String tokenName) {
        return sharedUtilityService.getTokenEmailForApplicationToken(tokenName);
    }

    public String getTokenEmailForGlobalToken(String tokenName) {
        return sharedUtilityService.getTokenEmailForGlobalToken(tokenName);
    }

    /**
     * Create a new token and return the email of the token
     *
     * @return the email of the token
     */
    public List<String> createTokens(MockMvc mockMvc,
                                     Optional<String> userInfo,
                                     List<NewAuthenticationTokenDTO> authenticationTokens) {
        List<AuthenticationTokenDTO> createdTokens = new ArrayList<>();
        for (NewAuthenticationTokenDTO newAuthenticationTokenDTO : authenticationTokens) {
            var newAuthenticationTokenApiResult = assertDoesNotThrow(
                    () -> createNewAuthenticationToken(
                            mockMvc,
                            status().isCreated(),
                            userInfo,
                            newAuthenticationTokenDTO
                    )
            );
            assertThat(newAuthenticationTokenApiResult)
                    .isNotNull()
                    .extracting(
                            ApiResultResponse::getErrorCode
                    )
                    .isEqualTo(0);
            createdTokens.add(newAuthenticationTokenApiResult.getPayload());
        }
        return createdTokens.stream().map(AuthenticationTokenDTO::email).toList();
    }

    public ApiResultResponse<String> getNewLogbookWithNameWithAuthorizationAndAppToken(
            MockMvc mockMvc,
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
                        status().isOk(),
                        userInfo,
                        newLogbookApiResult.getPayload(),
                        UpdateLogbookDTO
                                .builder()
                                .name(logbookName)
                                .shifts(Collections.emptyList())
                                .tags(Collections.emptyList())
                                .authorizations(
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

    public Optional<AuthenticationTokenDTO> getAuthenticationTokenByLogbookIdAndTokenName(String logbookId, String tokenName) {
        return assertDoesNotThrow(() -> logbookService.getAuthenticationTokenByName(logbookId, tokenName));
    }

    public ApiResultResponse<String> newAttachment(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            MockMultipartFile file) throws Exception {
        var requestBuilder = multipart("/v1/attachment").file(file);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result_upload = mockMvc.perform(
                        requestBuilder
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

    public void checkDownloadedFile(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String attachmentID,
            String mediaType) throws Exception {
        var requestBuilder = get("/v1/attachment/{id}/download", attachmentID)
                .contentType(mediaType);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        requestBuilder
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

    public void checkDownloadedPreview(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String attachmentID,
            String mediaType) throws Exception {
        var requestBuilder = get("/v1/attachment/{id}/preview.jpg", attachmentID)
                .contentType(mediaType);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        requestBuilder
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

//        if(userInfo.isPresent() && userInfo.get().toLowerCase().compareTo("service")==0) {
//            postBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateServiceToken());
//        } else {
        userInfo.ifPresent(login -> postBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
//        }

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

    public ApiResultResponse<String> importEntryV1(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            EntryImportDTO entryToImport,
            MockMultipartFile... files) throws Exception {
        MockMultipartHttpServletRequestBuilder multiPartBuilder = multipart("/v1/import");
        userInfo.ifPresent(login -> multiPartBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
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

    public ApiResultResponse<String> importEntryV2(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            ImportEntryDTO importEntryDTO,
            MockMultipartFile... files) throws Exception {
        MockMultipartHttpServletRequestBuilder multiPartBuilder = multipart("/v2/import");
        if (importEntryDTO != null) {
            MockPart p = new MockPart(
                    "entry",
                    new ObjectMapper().writeValueAsString(importEntryDTO).getBytes(StandardCharsets.UTF_8)
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
        userInfo.ifPresent(login -> multiPartBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));

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

        userInfo.ifPresent(login -> postBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));

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
        userInfo.ifPresent(login -> postBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
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

    public ApiResultResponse<List<EntrySummaryDTO>> getAllFollowUpLog(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String followedLogID) throws Exception {
        var getBuilder = get(
                "/v1/entries/{id}/follow-ups",
                followedLogID)
                .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
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

    public ApiResultResponse<EntryDTO> getFullLog(
            MockMvc mockMvc,
            Optional<String> userInfo,
            String id
    ) throws Exception {
        var getBuilder = get("/v1/entries/{id}", id)
                .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        getBuilder
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

    public ApiResultResponse<EntryDTO> getFullLog(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String id) throws Exception {
        var getBuilder = get("/v1/entries/{id}", id)
                .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
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

    public ApiResultResponse<EntryDTO> getFullLog(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String id,
            boolean includeFollowUps) throws Exception {
        return getFullLog(mockMvc, resultMatcher, userInfo, id, includeFollowUps, false, false);
    }

    public ApiResultResponse<EntryDTO> getFullLog(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String id, boolean includeFollowUps, boolean includeFollowingUps, boolean includeHistory) throws Exception {
        return getFullLog(mockMvc, resultMatcher, userInfo, id, includeFollowUps, includeFollowingUps, includeHistory, false, false);
    }

    public ApiResultResponse<EntryDTO> getFullLog(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String id,
            boolean includeFollowUps,
            boolean includeFollowingUps,
            boolean includeHistory,
            boolean includeReferences,
            boolean includeReferencedBy) throws Exception {
        MockHttpServletRequestBuilder getBuilder = get("/v1/entries/{id}", id)
                .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        if (includeFollowUps) {
            getBuilder.param("includeFollowUps", String.valueOf(true));
        }
        if (includeFollowingUps) {
            getBuilder.param("includeFollowingUps", String.valueOf(true));
        }
        if (includeHistory) {
            getBuilder.param("includeHistory", String.valueOf(true));
        }
        if (includeReferences) {
            getBuilder.param("includeReferences", String.valueOf(true));
        }
        if (includeReferencedBy) {
            getBuilder.param("includeReferencedBy", String.valueOf(true));
        }
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

    public ApiResultResponse<List<EntrySummaryDTO>> getReferencesByEntryId(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String id) throws Exception {
        MockHttpServletRequestBuilder getBuilder = get("/v1/entries/{id}/references", id)
                .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
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

    public ApiResultResponse<List<EntrySummaryDTO>> submitSearchByGetWithAnchor(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            Optional<String> anchorId,
            Optional<LocalDateTime> startDate,
            Optional<LocalDateTime> endDate,
            Optional<Integer> contextSize,
            Optional<Integer> limit,
            Optional<String> search,
            Optional<List<String>> tags,
            Optional<Boolean> requireAllTags,
            Optional<List<String>> logBook,
            Optional<Boolean> sortByLogDate,
            Optional<String> originId) throws Exception {

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
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        originId.ifPresent(id -> getBuilder.param("originId", id));
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
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
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
            Optional<String> filterForAuthorizationTypes
    ) throws Exception {

        MockHttpServletRequestBuilder getBuilder =
                get("/v1/logbooks")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        includeAuthorizations.ifPresent(b -> getBuilder.param("includeAuthorizations", String.valueOf(b)));
        filterForAuthorizationTypes.ifPresent(authType -> getBuilder.param("filterForAuthorizationTypes", authType));
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
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
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

    /**
     * Get logbook by id
     *
     * @param mockMvc               MockMvc
     * @param resultMatcher         ResultMatcher
     * @param userInfo              Optional<String>
     * @param logbookID             String
     * @param includeAuthorizations Optional<Boolean>
     * @return ApiResultResponse<LogbookDTO>
     * @throws Exception Exception
     */
    public ApiResultResponse<LogbookDTO> getLogbookByID(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String logbookID,
            Optional<Boolean> includeAuthorizations) throws Exception {
        MockHttpServletRequestBuilder getBuilder =
                get("/v1/logbooks/{logbookId}", logbookID)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        includeAuthorizations.ifPresent(b -> getBuilder.param("includeAuthorizations", String.valueOf(b)));
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

        userInfo.ifPresent(login -> postBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));

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
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
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
            Optional<String> userInfo,
            Optional<List<String>> logbooksName) throws Exception {
        MockHttpServletRequestBuilder getBuilder = get("/v1/tags")
                .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        logbooksName.ifPresent(lb -> {
            String[] lbArray = new String[lb.size()];
            lb.toArray(lbArray);
            getBuilder.param("logbooks", lbArray);
        });

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

        userInfo.ifPresent(login -> putBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));

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
        userInfo.ifPresent(login -> putBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
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

    /**
     * Get logbook authorizations
     * @param mockMvc
     * @param resultMatcher
     * @param userInfo
     * @return
     * @throws Exception
     */
    public ApiResultResponse<List<LogbookAuthorizationDTO>> getLogbookAuthorizationForCurrentUsers(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get("/v1/logbook/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Get logbook authorizations
     * @param mockMvc
     * @param resultMatcher
     * @param userInfo
     * @param logbookId
     * @return
     * @throws Exception
     */
    public ApiResultResponse<List<LogbookAuthorizationDTO>> getLogbookAuthorizationForCurrentUsers(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String logbookId) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get("/v1/logbook/{logbookId}/auth", logbookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    /**
     * Update logbook
     * @param mockMvc MockMvc
     * @param resultMatcher ResultMatcher
     * @param userInfo optional user information
     * @param logbookUserAuthorizationDTO LogbookUserAuthorizationDTO
     * @return ApiResultResponse<Boolean>
     * @throws Exception Exception
     */
    public ApiResultResponse<Boolean> applyLogbookUserAuthorization(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            List<LogbookUserAuthorizationDTO> logbookUserAuthorizationDTO) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post("/v1/logbook/auth/user")
                        .content(
                                objectMapper.writeValueAsString(logbookUserAuthorizationDTO)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<Boolean> applyLogbookUserAuthorizations(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String userId,
            List<LogbookAuthorizationDTO>  logbookAuthorization) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post("/v1/logbook/auth/user/{userId}", userId)
                        .content(
                                objectMapper.writeValueAsString(logbookAuthorization)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<Boolean> deleteLogbookUsersAuthorizations(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String logbookId) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                delete("/v1/logbook/{logbookId}/auth/user", logbookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<Boolean> applyLogbookGroupAuthorizations(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            List<LogbookGroupAuthorizationDTO> logbookUserAuthorizationDTO) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post("/v1/logbook/auth/group")
                        .content(
                                objectMapper.writeValueAsString(logbookUserAuthorizationDTO)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<Boolean> applyLogbookGroupAuthorizations(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String groupId,
            List<LogbookAuthorizationDTO> logbookAuthorization) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post("/v1/logbook/auth/group/{groupId}", groupId)
                        .content(
                                objectMapper.writeValueAsString(logbookAuthorization)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<Boolean> deleteLogbookGroupAuthorization(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String logbookId) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                delete("/v1/logbook/{logbookId}/auth/group", logbookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);
        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }

    public ApiResultResponse<String> findSummaryIdByShiftNameAndDate(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String shiftId,
            LocalDate date
    ) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get(
                        "/v1/entries/{shiftId}/summaries/{date}",
                        shiftId,
                        date
                )
                        .accept(MediaType.APPLICATION_JSON);

        return executeHttpRequest(
                new TypeReference<>() {
                },
                mockMvc,
                resultMatcher,
                userInfo,
                requestBuilder
        );
    }


    public ApiResultResponse<AuthenticationTokenDTO> createNewAuthenticationToken(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            NewAuthenticationTokenDTO newAuthenticationTokenDTO) throws Exception {
        MockHttpServletRequestBuilder postBuilder =
                post("/v1/auth/application-token")
                        .content(
                                new ObjectMapper().writeValueAsString(
                                        newAuthenticationTokenDTO
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> postBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
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

    public ApiResultResponse<List<AuthenticationTokenDTO>> getAllAuthenticationToken(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo) throws Exception {
        MockHttpServletRequestBuilder getBuilder =
                get("/v1/auth/application-token")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> getBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
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

    public ApiResultResponse<Boolean> deleteAuthenticationToken(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String id) throws Exception {
        MockHttpServletRequestBuilder deleteBuilder =
                delete("/v1/auth/application-token/{id}", id)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> deleteBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        deleteBuilder
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

    public ApiResultResponse<Boolean> createNewRootUser(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String userEmail) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                post("/v1/auth/root/{email}", userEmail)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        requestBuilder
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

    public ApiResultResponse<Boolean> deleteRootUser(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            String userEmail) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                delete("/v1/auth/root/{email}", userEmail)
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        requestBuilder
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

    public ApiResultResponse<List<AuthorizationDTO>> findAllRootUser(
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo) throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get("/v1/auth/root")
                        .accept(MediaType.APPLICATION_JSON);
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(
                        requestBuilder
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

    public ApiResultResponse<LogbookDTO> getTestLogbook(MockMvc mockMvc) {
        return getTestLogbook(mockMvc, "user1@slac.stanford.edu");
    }

    public ApiResultResponse<LogbookDTO> getTestLogbook(MockMvc mockMvc, String whitUserEmail) {
        ApiResultResponse<String> logbookCreationResult = assertDoesNotThrow(
                () -> createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        Optional.of(
                                whitUserEmail
                        ),
                        NewLogbookDTO
                                .builder()
                                .name(UUID.randomUUID().toString())
                                .build()
                ));
        Assertions.assertThat(logbookCreationResult).isNotNull();
        Assertions.assertThat(logbookCreationResult.getErrorCode()).isEqualTo(0);
        return assertDoesNotThrow(
                () -> getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        logbookCreationResult.getPayload()
                )
        );
    }

    public <T> ApiResultResponse<T> executeHttpRequest(
            TypeReference<ApiResultResponse<T>> typeRef,
            MockMvc mockMvc,
            ResultMatcher resultMatcher,
            Optional<String> userInfo,
            MockHttpServletRequestBuilder requestBuilder) throws Exception {
        userInfo.ifPresent(login -> requestBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));

        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(resultMatcher)
                .andReturn();

        if (result.getResolvedException() != null) {
            throw result.getResolvedException();
        }
        return objectMapper.readValue(result.getResponse().getContentAsString(), typeRef);
    }
}
