package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.service.EntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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
    public ApiResultResponse<String> newLog(@RequestBody EntryNewDTO newEntry) {
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
    public ApiResultResponse<String> newSupersedeLog(@PathVariable String id, @RequestBody EntryNewDTO newLog) {
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
    public ApiResultResponse<String> newFollowupLog(@PathVariable String id, @RequestBody EntryNewDTO newLog) {
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
    public ApiResultResponse<List<EntrySummaryDTO>> getAllFollowUpLog(@PathVariable String id) {
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
    public ApiResultResponse<EntryDTO> getFullLog(
            @PathVariable String id,
            @Parameter(name = "includeFollowUps", description = "If true the API return all the followUps")
            @RequestParam("includeFollowUps") Optional<Boolean> includeFollowUps,
            @Parameter(name = "includeFollowingUps", description = "If true the API return all the followingUp")
            @RequestParam("includeFollowingUps") Optional<Boolean> includeFollowingUps,
            @Parameter(name = "includeHistory", description = "If true the API return all log updates history")
            @RequestParam("includeHistory") Optional<Boolean> includeHistory
    ) {
        return ApiResultResponse.of(
                entryService.getFullLog(id, includeFollowUps, includeFollowingUps, includeHistory)
        );
    }

    @GetMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(
            description = "Perform the query on all log data",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful response"
//                            content = @Content(
//                                    array = @ArraySchema(
//                                            schema = @Schema(
//                                                    implementation = EntrySummaryDTO.class
//                                            )
//                                    )
//                            )
                    )
            }
    )
    public ApiResultResponse<List<EntrySummaryDTO>> search(
            @Parameter(name = "startDate", description = "Only include entries after this date. Defaults to current time.")
            @RequestParam("startDate") Optional<LocalDateTime> startDate,
            @Parameter(name = "endDate", description = "Only include entries before this date. If not supplied, then does not apply any filter")
            @RequestParam("endDate") Optional<LocalDateTime> endDate,
            @Parameter(name = "contextSize", description = "Include this number of entries before the startDate (used for highlighting entries)")
            @RequestParam("contextSize") Optional<Integer> contextSize,
            @Parameter(name = "limit", description = "Limit the number the number of entries after the start date.")
            @RequestParam(value = "limit") Optional<Integer>  limit,
            @Parameter(name = "search", description = "Typical search functionality")
            @RequestParam("search") Optional<String> search,
            @Parameter(name = "tags", description = "Only include entries that use one of these tags")
            @RequestParam("tags") Optional<List<String>> tags,
            @Parameter(name = "logbooks", description = "Only include entries that belong to one of these logbooks")
            @RequestParam("logbooks") Optional<List<String>> logBook) {
        return ApiResultResponse.of(
                entryService.searchAll(
                        QueryWithAnchorDTO
                                .builder()
                                .startDate(startDate.orElse(null))
                                .endDate(endDate.orElse(null))
                                .contextSize(contextSize.orElse(0))
                                .limit(limit.orElse(0))
                                .search(search.orElse(null))
                                .tags(tags.orElse(Collections.emptyList()))
                                .logbooks(logBook.orElse(Collections.emptyList()))
                                .build()
                )
        );
    }
}
