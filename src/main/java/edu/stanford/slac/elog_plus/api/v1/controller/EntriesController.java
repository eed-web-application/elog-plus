package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.service.EntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController()
@RequestMapping("/v1/entries")
@AllArgsConstructor
@Schema(description = "Main set of api for the query on the log data")
public class EntriesController {
    private EntryService entryService;

    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(description = "Perform the query on all log data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<String> newEntry(@RequestBody EntryNewDTO newEntry) {
        return ApiResultResponse.of(
                entryService.createNew(newEntry)
        );
    }

    @PostMapping(
            path = "/{id}/supersede",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Create a new supersede for the log identified by the id")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<String> newSupersede(@PathVariable String id, @RequestBody EntryNewDTO newLog) {
        return ApiResultResponse.of(
                entryService.createNewSupersede(id, newLog)
        );
    }

    @PostMapping(
            path = "/{id}/follow-ups",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Create a new follow-up log for the the log identified by the id")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<String> newFollowup(@PathVariable String id, @RequestBody EntryNewDTO newLog) {
        return ApiResultResponse.of(
                entryService.createNewFollowUp(id, newLog)
        );
    }

    @GetMapping(
            path = "/{id}/follow-ups",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Return all the follow-up logs for a specific log identified by the id")
    @ResponseStatus(HttpStatus.OK)
    public ApiResultResponse<List<EntrySummaryDTO>> getAllFollowUp(@PathVariable String id) {
        return ApiResultResponse.of(
                entryService.getAllFollowUpForALog(id)
        );
    }

    @GetMapping(
            path = "/{id}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Return the full log information")
    @ResponseStatus(HttpStatus.OK)
    public ApiResultResponse<EntryDTO> getFull(
            @PathVariable String id,
            @Parameter(name = "includeFollowUps", description = "If true the API return all the followUps")
            @RequestParam("includeFollowUps") Optional<Boolean> includeFollowUps,
            @Parameter(name = "includeFollowingUps", description = "If true the API return all the followingUp")
            @RequestParam("includeFollowingUps") Optional<Boolean> includeFollowingUps,
            @Parameter(name = "includeHistory", description = "If true the API return all log updates history")
            @RequestParam("includeHistory") Optional<Boolean> includeHistory
    ) {
        return ApiResultResponse.of(
                entryService.getFullEntry(id, includeFollowUps, includeFollowingUps, includeHistory)
        );
    }

    @GetMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(
            description = "Perform the query on all log data"
    )
    @ResponseStatus(HttpStatus.OK)
    public ApiResultResponse<List<EntrySummaryDTO>> search(
            @Parameter(name = "anchorId", description = "Is the id of an entry from where start the search")
            @RequestParam("anchorId") Optional<String> anchorId,
            @Parameter(name = "startDate", description = "Only include entries after this date. Defaults to current time.")
            @RequestParam("startDate") Optional<LocalDateTime> startDate,
            @Parameter(name = "endDate", description = "Only include entries before this date. If not supplied, then does not apply any filter")
            @RequestParam("endDate") Optional<LocalDateTime> endDate,
            @Parameter(name = "contextSize", description = "Include this number of entries before the startDate (used for highlighting entries)")
            @RequestParam("contextSize") Optional<Integer> contextSize,
            @Parameter(name = "limit", description = "Limit the number the number of entries after the start date.")
            @RequestParam(value = "limit") Optional<Integer> limit,
            @Parameter(name = "search", description = "Typical search functionality")
            @RequestParam("search") Optional<String> search,
            @Parameter(name = "tags", description = "Only include entries that use one of these tags")
            @RequestParam("tags") Optional<List<String>> tags,
            @Parameter(name = "logbooks", description = "Only include entries that belong to one of these logbooks")
            @RequestParam("logbooks") Optional<List<String>> logBook,
            @Parameter(name = "sortByLogDate", description = "Sort entries by log date instead event date")
            @RequestParam(value = "sortByLogDate", defaultValue = "false") Optional<Boolean> sortByLogDate,
            @Parameter(name = "hideSummaries", description = "Hide the summaries from the search(default is false)")
            @RequestParam(value = "hideSummaries", defaultValue = "false") Optional<Boolean> hideSummaries) {
        return ApiResultResponse.of(
                entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .anchorID(anchorId.orElse(null))
                                .startDate(startDate.orElse(null))
                                .endDate(endDate.orElse(null))
                                .contextSize(contextSize.orElse(0))
                                .limit(limit.orElse(0))
                                .search(search.orElse(null))
                                .tags(tags.orElse(Collections.emptyList()))
                                .logbooks(logBook.orElse(Collections.emptyList()))
                                .sortByLogDate(sortByLogDate.orElse(false))
                                .hideSummaries(hideSummaries.orElse(false))
                                .build()
                )
        );
    }

    @GetMapping(
            path = "/{shiftId}/summaries/{date}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(
            description = "Perform the query on all log data"
    )
    @ResponseStatus(HttpStatus.OK)
    public ApiResultResponse<String> findSummaryForShiftAndDate(
            @PathVariable String shiftId,
            @PathVariable LocalDate date
    ) {
        return ApiResultResponse.of(
                entryService.findSummaryIdForShiftIdAndDate(
                        shiftId,
                        date
                )
        );
    }

}
