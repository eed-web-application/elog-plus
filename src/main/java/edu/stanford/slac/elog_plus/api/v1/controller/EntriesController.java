package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.NotAuthorized;
import edu.stanford.slac.elog_plus.service.AuthService;
import edu.stanford.slac.elog_plus.service.EntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static edu.stanford.slac.elog_plus.exception.Utility.*;
import static edu.stanford.slac.elog_plus.model.Authorization.Type.Read;
import static edu.stanford.slac.elog_plus.model.Authorization.Type.Write;

@RestController()
@RequestMapping("/v1/entries")
@AllArgsConstructor
@Schema(description = "Main set of api for the query on the log entries")
public class EntriesController {
    private AuthService authService;
    private EntryService entryService;

    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(description = "Perform the query on all log entries")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<String> newEntry(
            @RequestBody EntryNewDTO newEntry,
            Authentication authentication) {
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::newEntry")
                        .build(),
                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and can at least write the logbook which the entry belong
                () -> all(
                        newEntry.logbooks().stream()
                                .map(
                                        logbookId -> (Supplier<Boolean>) () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                authentication,
                                                AuthorizationType.Write,
                                                "/logbook/%s".formatted(logbookId)
                                        )
                                )
                                .toList()
                )
        );
        return ApiResultResponse.of(
                entryService.createNew(
                        newEntry,
                        authService.findPerson(authentication)
                )
        );
    }

    @PostMapping(
            path = "/{id}/supersede",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Create a new supersede for the log identified by the id")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<String> newSupersede(
            @PathVariable String id,
            @RequestBody EntryNewDTO newSupersedeEntry,
            Authentication authentication) {
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::newSupersede")
                        .build(),

                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and can at least write
                () -> all(
                        // write
                        newSupersedeEntry.logbooks().stream()
                                .map(
                                        logbookId -> (Supplier<Boolean>) () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                authentication,
                                                AuthorizationType.Write,
                                                "/logbook/%s".formatted(logbookId)
                                        )
                                )
                                .toList()
                )
        );
        return ApiResultResponse.of(
                entryService.createNewSupersede(id, newSupersedeEntry)
        );
    }

    @PostMapping(
            path = "/{id}/follow-ups",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Create a new follow-up log for the the log identified by the id")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<String> newFollowUp(
            @PathVariable String id,
            @RequestBody EntryNewDTO newFollowUpEntry,
            Authentication authentication) {
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::newFollowUp")
                        .build(),

                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // or can at least write the logbook which the entry belong
                () -> all(
                        // write
                        newFollowUpEntry.logbooks().stream()
                                .map(
                                        logbookId -> (Supplier<Boolean>) () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                authentication,
                                                AuthorizationType.Write,
                                                "/logbook/%s".formatted(logbookId)
                                        )
                                )
                                .toList()
                )
        );
        return ApiResultResponse.of(
                entryService.createNewFollowUp(
                        id,
                        newFollowUpEntry,
                        authService.findPerson(authentication)
                )
        );
    }

    @GetMapping(
            path = "/{id}/follow-ups",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Return all the follow-up logs for a specific entry identified by the id")
    @ResponseStatus(HttpStatus.OK)
    public ApiResultResponse<List<EntrySummaryDTO>> getAllFollowUp(
            @PathVariable String id,
            Authentication authentication) {
        // check for authorizations
        assertion(
                () -> authService.checkAuthentication(authentication),
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::getAllFollowUp")
                        .build()
        );
        final boolean userIsRoot = authService.checkForRoot(authentication);
        // fetch all follow up
        return ApiResultResponse.of(
                filterEntrySummaryByAuthentication(
                        entryService.getAllFollowUpForALog(id),
                        authentication
                )
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
            @Parameter(name = "includeFollowUps", description = "If true the API return all the entries that are follow-up of this one")
            @RequestParam("includeFollowUps") Optional<Boolean> includeFollowUps,
            @Parameter(name = "includeFollowingUps", description = "If true the API return all the entries that are follow-up of this one")
            @RequestParam("includeFollowingUps") Optional<Boolean> includeFollowingUps,
            @Parameter(name = "includeHistory", description = "If true the API return all the entry updates history")
            @RequestParam("includeHistory") Optional<Boolean> includeHistory,
            @Parameter(name = "includeReferenceBy", description = "If true the API return all the entries that are referenced by this one")
            @RequestParam("includeReferences") Optional<Boolean> includeReferences,
            @Parameter(name = "includeReferencedBy", description = "If true the API return all the entries that are referenced byt this one")
            @RequestParam("includeReferencedBy") Optional<Boolean> includeReferencedBy,
            Authentication authentication
    ) {
        List<String> lbForTheEntry = entryService.getLogbooksForAnEntryId(id);
        // filter the unauthorized logbook id
        List<String> authorizedEntryLogbook = lbForTheEntry.stream().filter(
                lbId -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        AuthorizationType.Read,
                        "/logbook/%s".formatted(lbId)
                )
        ).toList();

        // check among all others authorizations
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::getFull")
                        .build(),
                // is authenticated
                () -> authService.checkAuthentication(authentication),
                //and
                () -> any(
                        // or is administrator
                        () -> authService.checkForRoot(authentication),
                        // or is authorized at least in on e logbook to read
                        () -> !authorizedEntryLogbook.isEmpty()
                )
        );

        EntryDTO foundEntry = entryService.getFullEntry(
                id,
                includeFollowUps,
                includeFollowingUps,
                includeHistory,
                includeReferences,
                includeReferencedBy
        );

        //we have to filter out the logbook not authorized
        List<LogbookSummaryDTO> authorizedLogbookSummary = foundEntry.logbooks()
                .stream()
                .filter(
                        lb -> authorizedEntryLogbook.contains(lb.id())
                ).toList();
        // return entry with authorized only logbook summary
        return ApiResultResponse.of(
                foundEntry.toBuilder()
                        .logbooks(
                                authorizedLogbookSummary
                        )
                        .build()
        );
    }

    @GetMapping(
            path = "/{id}/references",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Return the full log information")
    @ResponseStatus(HttpStatus.OK)
    public ApiResultResponse<List<EntrySummaryDTO>> getAllTheReferences(
            @Parameter(description = "Is the id of the entry for wich we want to load all the reference to")
            @PathVariable String id,
            Authentication authentication) {
        //filter all readable logbook wich the entry belongs
        final List<String> authorizedEntryLogbook = entryService.getLogbooksForAnEntryId(id)
                .stream()
                .filter(
                        lbId -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                authentication,
                                AuthorizationType.Read,
                                "/logbook/%s".formatted(lbId)
                        )
                ).toList();

        // check authorization on
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("EntriesController::getAllTheReferences")
                        .build(),
                // need to be authenticated
                () -> authService.checkAuthentication(authentication),
                // ca read from at least one logbook
                () -> !authorizedEntryLogbook.isEmpty()
        );
        return ApiResultResponse.of(
                filterEntrySummaryByAuthentication(
                        entryService.getReferencesByEntryID(id),
                        authentication
                )
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
            @RequestParam(value = "hideSummaries", defaultValue = "false") Optional<Boolean> hideSummaries,
            @Parameter(name = "requireAllTags", description = "Require that all entries found includes all the tags")
            @RequestParam(value = "requireAllTags", defaultValue = "false") Optional<Boolean> requireAllTags) {
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
                                .requireAllTags(requireAllTags.orElse(false))
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

    private List<EntrySummaryDTO> filterEntrySummaryByAuthentication(List<EntrySummaryDTO> summaries, Authentication authentication) {
        return summaries
                .stream()
                .map(
                        entry -> {
                            // filter all authorized logbook for en entry
                            var authorizedLogbook = entry
                                    .logbooks()
                                    .stream()
                                    .filter(
                                            lb -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                    authentication,
                                                    AuthorizationType.Read,
                                                    "/logbook/%s".formatted(lb.id())
                                            )
                                    )
                                    .toList();
                            // recreate the summary with the authorized logbook
                            return entry.toBuilder()
                                    .logbooks(
                                            authorizedLogbook
                                    )
                                    .build();
                        }
                )
                .filter(
                        // remove all summary for which the user is unauthorized on all logbook
                        lb -> !lb.logbooks().isEmpty()
                )
                .toList();
    }
}
