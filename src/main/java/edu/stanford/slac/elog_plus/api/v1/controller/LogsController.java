package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.service.LogService;
import edu.stanford.slac.elog_plus.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController()
@RequestMapping("/v1/logs")
@AllArgsConstructor
@Schema(description = "Main set of api for the query on the log data")
public class LogsController {
    private LogService logService;
    private TagService tagService;

    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(description = "Perform the query on all log data")
    public ApiResultResponse<String> newLog(@RequestBody NewLogDTO newLog) {
        return ApiResultResponse.of(
                logService.createNew(newLog)
        );
    }

    @PostMapping(
            path = "/{id}/supersede",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Create a new supersede for the log identified by the id")
    public ApiResultResponse<String> newSupersedeLog(@PathVariable String id, @RequestBody NewLogDTO newLog) {
        return ApiResultResponse.of(
                logService.createNewSupersede(id, newLog)
        );
    }

    @PostMapping(
            path = "/{id}/follow-up",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Create a new follow-up log for the the log identified by the id")
    public ApiResultResponse<String> newFollowupLog(@PathVariable String id, @RequestBody NewLogDTO newLog) {
        return ApiResultResponse.of(
                logService.createNewFollowUp(id, newLog)
        );
    }

    @GetMapping(
            path = "/{id}/follow-up",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Return all the follow-up logs for a specific log identified by the id")
    public ApiResultResponse<List<SearchResultLogDTO>> getAllFollowUpLog(@PathVariable String id) {
        return ApiResultResponse.of(
                logService.getAllFollowUpForALog(id)
        );
    }

    @GetMapping(
            path = "/{id}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Return the full log information")
    public ApiResultResponse<LogDTO> newLog(
            @PathVariable String id,
            @Parameter(name = "includeFollowUps", description = "If true the API return all the followUp")
            @RequestParam("includeFollowUps") Optional<Boolean> includeFollowUps
    ) {
        return ApiResultResponse.of(
                logService.getFullLog(id, includeFollowUps)
        );
    }

    @GetMapping(
            path = "/paging",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Perform the query on all log data")
    public ApiResultResponse<QueryPagedResultDTO<SearchResultLogDTO>> search(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("logbook") List<String> logBook) {
        return ApiResultResponse.of(
                logService.searchAll(
                        QueryParameterDTO
                                .builder()
                                .page(page)
                                .rowPerPage(size)
                                .logBook(logBook)
                                .build()
                )
        );
    }

    @GetMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Perform the query on all log data")
    public ApiResultResponse<List<SearchResultLogDTO>> search(
            @Parameter(name = "anchorDate", description = "is the date for which the search is started")
            @RequestParam("anchorDate") Optional<LocalDateTime> anchorDate,
            @Parameter(name = "logsBefore", description = "the number of the log before the anchor date to return")
            @RequestParam("logsBefore") Optional<Integer> logsBefore,
            @Parameter(name = "logsAfter", description = "the number of the log after the anchor date to return, thi work also without anchor date [return the first page]")
            @RequestParam("logsAfter") Optional<Integer> logsAfter,
            @Parameter(name = "logbook", description = "a set of the logbook to include in the search")
            @RequestParam("logbook") Optional<List<String>> logBook) {
        return ApiResultResponse.of(
                logService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .anchorDate(anchorDate.orElse(null))
                                .logsBefore(logsBefore.orElse(0))
                                .logsAfter(logsAfter.orElse(0))
                                .logBook(logBook.orElse(Collections.emptyList()))
                                .build()
                )
        );
    }
}
