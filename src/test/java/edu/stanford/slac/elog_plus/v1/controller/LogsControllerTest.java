package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.model.Log;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.api.AssertionsForInterfaceTypes;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

        var queryResult = testHelperService.submitSearchByGetWithAnchor(
                mockMvc,
                Optional.empty(),
                Optional.empty(),
                Optional.of(10),
                Optional.of(List.of("MCC"))
        );
        AssertionsForClassTypes.assertThat(queryResult.getPayload().size()).isEqualTo(1);
    }

    @Test
    public void createNewLogAndSearchWithPaging() throws Exception {
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

        var queryResult = testHelperService.submitSearchByGet(
                mockMvc,
                0,
                10,
                List.of("MCC")
        );
        AssertionsForClassTypes.assertThat(queryResult.getPayload().getContent().size()).isEqualTo(1);
    }

    @Test
    public void fetchFullLog() {
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
        AssertionsForClassTypes.assertThat(newLogID.getErrorCode()).isEqualTo(0);

        ApiResultResponse<LogDTO> fullLog = assertDoesNotThrow(
                () ->
                        testHelperService.getFullLog(
                                mockMvc,
                                newLogID.getPayload()
                        )
        );
        AssertionsForClassTypes.assertThat(newLogID.getErrorCode()).isEqualTo(0);
        AssertionsForClassTypes.assertThat(fullLog.getPayload().id()).isEqualTo(newLogID.getPayload());
    }

    @Test
    public void createNewSupersedeLog() throws Exception {
        ApiResultResponse<String> newLogIDResult =
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

        AssertionsForClassTypes.assertThat(newLogIDResult).isNotNull();
        AssertionsForClassTypes.assertThat(newLogIDResult.getErrorCode()).isEqualTo(0);
        //create supersede
        ApiResultResponse<String> newSupersedeLogIDResult =assertDoesNotThrow(
                () -> testHelperService.createNewSupersedeLog(
                        mockMvc,
                        newLogIDResult.getPayload(),
                        NewLogDTO
                                .builder()
                                .logbook("MCC")
                                .text("This is a log for test")
                                .title("A very wonderful supersede log")
                                .build()
                )
        );

        //check old log for supersede info
        ApiResultResponse<LogDTO> oldFull = assertDoesNotThrow(
                () ->
                        testHelperService.getFullLog(
                                mockMvc,
                                newLogIDResult.getPayload()
                        )
        );

        assertThat(oldFull.getErrorCode()).isEqualTo(0);
        assertThat(oldFull.getPayload().supersedeBy()).isEqualTo(newSupersedeLogIDResult.getPayload());

        // the search api now should return only the new log and not the superseded on
        var queryResult = testHelperService.submitSearchByGet(mockMvc, 0, 5, Collections.emptyList());
        AssertionsForClassTypes.assertThat(queryResult).isNotNull();
        AssertionsForClassTypes.assertThat(queryResult.getPayload().getContent().size()).isEqualTo(1);
    }


    @Test
    public void createNewFollowUpLogsAndFetch() throws Exception {
        ApiResultResponse<String> newLogIDResult =
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

        AssertionsForClassTypes.assertThat(newLogIDResult).isNotNull();
        AssertionsForClassTypes.assertThat(newLogIDResult.getErrorCode()).isEqualTo(0);

        //create follow-up
        ApiResultResponse<String> newFULogIDOneResult =assertDoesNotThrow(
                () -> testHelperService.createNewFollowUpLog(
                        mockMvc,
                        newLogIDResult.getPayload(),
                        NewLogDTO
                                .builder()
                                .logbook("MCC")
                                .text("This is a log for test")
                                .title("A very wonderful followup log one")
                                .build()
                )
        );

        //create follow-up
        ApiResultResponse<String> newFULogIDTwoResult =assertDoesNotThrow(
                () -> testHelperService.createNewFollowUpLog(
                        mockMvc,
                        newLogIDResult.getPayload(),
                        NewLogDTO
                                .builder()
                                .logbook("MCC")
                                .text("This is a log for test")
                                .title("A very wonderful followup log one")
                                .build()
                )
        );

        ApiResultResponse<List<SearchResultLogDTO>> foundFollowUp = assertDoesNotThrow(
                () -> testHelperService.getAllFollowUpLog(
                        mockMvc,
                        newLogIDResult.getPayload()
                )
        );
        assertThat(foundFollowUp.getErrorCode()).isEqualTo(0);
        assertThat(foundFollowUp.getPayload().size()).isEqualTo(2);

        //get full log without followUPs
        ApiResultResponse<LogDTO> fullLog =assertDoesNotThrow(
                () -> testHelperService.getFullLog(
                        mockMvc,
                        newLogIDResult.getPayload(),
                        false
                )
        );

        assertThat(fullLog.getErrorCode()).isEqualTo(0);
        assertThat(fullLog.getPayload().followUp()).isNull();

        //get full log without followUPs
        ApiResultResponse<LogDTO> fullLogWitFollowUps =assertDoesNotThrow(
                () -> testHelperService.getFullLog(
                        mockMvc,
                        newLogIDResult.getPayload(),
                        true
                )
        );

        assertThat(fullLogWitFollowUps.getErrorCode()).isEqualTo(0);
        assertThat(fullLogWitFollowUps.getPayload().followUp()).isNotNull();
        assertThat(fullLogWitFollowUps.getPayload().followUp().size()).isEqualTo(2);
    }


    @Test
    public void searchWithAnchor() {
        // create some data
        for (int idx = 0; idx < 100; idx++) {
            int finalIdx = idx;
            ApiResultResponse<String> newLogID =
                    assertDoesNotThrow(
                            () -> testHelperService.createNewLog(
                                    mockMvc,
                                    NewLogDTO
                                            .builder()
                                            .logbook("MCC")
                                            .text("This is a log for test")
                                            .title("A very wonderful log")
                                            .segment(String.valueOf(finalIdx))
                                            .build()
                            )
                    );
            AssertionsForClassTypes.assertThat(newLogID).isNotNull();
        }

        // initial search
        ApiResultResponse<List<SearchResultLogDTO>> firstPageResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty()
                )
        );
        AssertionsForInterfaceTypes.assertThat(firstPageResult.getErrorCode()).isEqualTo(0);
        List<SearchResultLogDTO> firstPage = firstPageResult.getPayload();
        AssertionsForInterfaceTypes.assertThat(firstPage).isNotNull();
        AssertionsForClassTypes.assertThat(firstPage.size()).isEqualTo(10);
        String lastSegment = null;
        for (SearchResultLogDTO l:
                firstPage) {
            if(lastSegment == null) {
                lastSegment = l.segment();
                continue;
            }
            AssertionsForClassTypes.assertThat(Integer.valueOf(lastSegment)).isGreaterThan(
                    Integer.valueOf(l.segment())
            );
            lastSegment = l.segment();
        }
        var testAnchorDate = firstPage.get(firstPage.size()-1).logDate();
        // load next page
        ApiResultResponse<List<SearchResultLogDTO>> nextPageResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        Optional.of(testAnchorDate),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty()
                )
        );
        AssertionsForInterfaceTypes.assertThat(nextPageResult.getErrorCode()).isEqualTo(0);
        List<SearchResultLogDTO> nextPage = nextPageResult.getPayload();
        AssertionsForInterfaceTypes.assertThat(nextPage).isNotNull();
        AssertionsForClassTypes.assertThat(nextPage.size()).isEqualTo(10);
        // check that the first of next page is not the last of the previous
        AssertionsForClassTypes.assertThat(nextPage.get(0).id()).isNotEqualTo(firstPage.get(firstPage.size()-1).id());

        lastSegment = nextPage.get(0).segment();
        nextPage.remove(0);
        for (SearchResultLogDTO l:
                nextPage) {
            AssertionsForClassTypes.assertThat(Integer.valueOf(lastSegment)).isGreaterThan(
                    Integer.valueOf(l.segment())
            );
            lastSegment = l.segment();
        }

        // now get all the record upside and downside
        ApiResultResponse<List<SearchResultLogDTO>> prevPageByPinResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        Optional.of(testAnchorDate),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        AssertionsForInterfaceTypes.assertThat(prevPageByPinResult.getErrorCode()).isEqualTo(0);
        List<SearchResultLogDTO> prevPageByPin = prevPageByPinResult.getPayload();
        AssertionsForInterfaceTypes.assertThat(prevPageByPin).isNotNull();
        AssertionsForClassTypes.assertThat(prevPageByPin.size()).isEqualTo(10);
        AssertionsForInterfaceTypes.assertThat(prevPageByPin).isEqualTo(firstPage);

        // test prev and next
        ApiResultResponse<List<SearchResultLogDTO>> prevAndNextPageByPinResult = assertDoesNotThrow(
                () -> testHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        Optional.of(testAnchorDate),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        AssertionsForInterfaceTypes.assertThat(prevAndNextPageByPinResult.getErrorCode()).isEqualTo(0);
        List<SearchResultLogDTO> prevAndNextPageByPin = prevAndNextPageByPinResult.getPayload();
        AssertionsForInterfaceTypes.assertThat(prevAndNextPageByPin).isNotNull();
        AssertionsForClassTypes.assertThat(prevAndNextPageByPin.size()).isEqualTo(20);
        AssertionsForInterfaceTypes.assertThat(prevAndNextPageByPin).containsAll(firstPage);
        AssertionsForInterfaceTypes.assertThat(prevAndNextPageByPin).containsAll(nextPage);
    }
}
