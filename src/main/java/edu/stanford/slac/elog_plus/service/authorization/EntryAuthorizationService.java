package edu.stanford.slac.elog_plus.service.authorization;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.LogbookNotAuthorized;
import edu.stanford.slac.elog_plus.service.EntryService;
import edu.stanford.slac.elog_plus.service.LogbookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Read;
import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Write;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.*;


@Service
@Validated
@AllArgsConstructor
public class EntryAuthorizationService {
    private final AuthService authService;
    private final EntryService entryService;
    private final LogbookService logbookService;

    public boolean canCreateNewEntry(Authentication authentication, @Valid EntryNewDTO newEntry) {
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::newEntry")
                        .build(),
                // and can at least write the logbook which the entry belong
                () -> all(
                        newEntry.logbooks().stream()
                                .map(
                                        logbookId -> (Supplier<Boolean>) () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                authentication,
                                                Write,
                                                "/logbook/%s".formatted(logbookId)
                                        )
                                )
                                .toList()
                )
        );
        return true;
    }

    public boolean canCreateSupersede(Authentication authentication, @NotNull String entryId, @Valid EntryNewDTO newSupersedeEntry) {
// check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::newSupersede")
                        .build(),
                // and can at least write
                () -> all(
                        // write
                        newSupersedeEntry.logbooks().stream()
                                .map(
                                        logbookId -> (Supplier<Boolean>) () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                authentication,
                                                Write,
                                                "/logbook/%s".formatted(logbookId)
                                        )
                                )
                                .toList()
                )
        );
        return true;
    }

    public boolean canCreateNewFollowUp(Authentication authentication, @NotNull String entryId, @Valid EntryNewDTO newFollowUpEntry) {
// check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::newFollowUp")
                        .build(),
                // or can at least write the logbook which the entry belong
                () -> all(
                        // write
                        newFollowUpEntry.logbooks().stream()
                                .map(
                                        logbookId -> (Supplier<Boolean>) () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                authentication,
                                                Write,
                                                "/logbook/%s".formatted(logbookId)
                                        )
                                )
                                .toList()
                )
        );
        return true;
    }

    public boolean canGetAllFollowUps(Authentication authentication, @NotNull String entryId) {
        // check for authorizations
        return true;
    }

    public boolean canGetFullEntry(Authentication authentication, @NotNull String entryId, AuthorizationCache authorizationCache) {
        // check for authorizations
        List<String> lbForTheEntry = entryService.getLogbooksForAnEntryId(entryId);
        // filter the unauthorized logbook id
        authorizationCache.setAuthorizedLogbookId(
                lbForTheEntry.stream().filter(
                        lbId -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                authentication,
                                Read,
                                "/logbook/%s".formatted(lbId)
                        )
                ).toList()
        );

        // check among all others authorizations
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::getFull")
                        .build(),
                //and
                () -> any(
                        () -> authService.checkForRoot(authentication),
                        // or is authorized at least in on e logbook to read
                        () -> !authorizationCache.getAuthorizedLogbookId().isEmpty()
                )
        );
        return true;
    }

    public boolean applyFilterAuthorizationEntryDTO(ApiResultResponse<EntryDTO> foundEntryResult, Authentication authentication, AuthorizationCache authorizationCache) {
        EntryDTO entry = foundEntryResult.getPayload();
        //we have to filter out the logbook not authorized
        List<LogbookSummaryDTO> authorizedLogbookSummary = entry.logbooks()
                .stream()
                .filter(
                        lb -> authorizationCache.getAuthorizedLogbookId().contains(lb.id())
                ).toList();
        foundEntryResult.setPayload(entry.toBuilder().logbooks(authorizedLogbookSummary).build());
        return true;
    }

    public boolean canGetAllReferences(Authentication authentication, @NotNull String entryId, AuthorizationCache authorizationCache) {
        //filter all readable logbook which the entry belongs
        authorizationCache.setAuthorizedLogbookId(
                entryService.getLogbooksForAnEntryId(entryId)
                        .stream()
                        .filter(
                                lbId -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                        authentication,
                                        Read,
                                        "/logbook/%s".formatted(lbId)
                                )
                        ).toList()
        );

        // check authorization on
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("EntriesController::getAllTheReferences")
                        .build(),
                // ca read from at least one logbook
                () -> !authorizationCache.getAuthorizedLogbookId().isEmpty()
        );
        return true;
    }

    public boolean applyFilterAuthorizationOnEntrySummaryDTOList(ApiResultResponse<List<EntrySummaryDTO>> foundSummaries, Authentication authentication, AuthorizationCache authorizationCache) {
        List<EntrySummaryDTO> entrySummaries = foundSummaries.getPayload();
        List<EntrySummaryDTO> updatedEntrySummaries = entrySummaries.stream()
                .map(entrySummary -> {
                    // Filter out the unauthorized logbooks
                    List<LogbookSummaryDTO> authorizedLogbookSummary = entrySummary.logbooks()
                            .stream()
                            .filter(lb -> authorizationCache.getAuthorizedLogbookId().isEmpty() || authorizationCache.getAuthorizedLogbookId().contains(lb.id()))
                            .toList();
                    // filter out tags not authorized
                    List<TagDTO> authorizedTag = entrySummary.tags()
                            .stream()
                            .filter(tag -> authorizationCache.getAuthorizedLogbookId().isEmpty() || authorizationCache.getAuthorizedLogbookId().contains(tag.logbook().id()))
                            .toList();
                    // Create a new EntrySummaryDTO with the filtered logbooks using toBuilder
                    return entrySummary.toBuilder()
                            .logbooks(authorizedLogbookSummary)
                            .tags(authorizedTag)
                            .build();
                })
                .toList();
        foundSummaries.setPayload(updatedEntrySummaries);
        return true;
    }

    public boolean canSearchEntry(Authentication authentication, Optional<List<String>> logBooks, AuthorizationCache authorizationCache) {
        // check authorization on
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("EntriesController::search")
                        .build(),
                // need to be authenticated
                () -> authService.checkAuthentication(authentication)
        );

        if (!authService.checkForRoot(authentication)) {
            // if user is not root whe t check for specific authorization
            authorizationCache.setAuthorizedLogbookId(
                    authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                                    authentication.getCredentials().toString(),
                                    Read,
                                    "/logbook/",
                                    Optional.empty()
                            ).stream().map(
                                    auth -> auth.resource().substring(
                                            auth.resource().lastIndexOf("/") + 1
                                    )
                            )
                            .toList()
            );

            if (logBooks.isPresent() && !logBooks.get().isEmpty()) {
                // filter out logbook id that are not authorized for the
                // current user
                logBooks.get().forEach(
                        lId -> {
                            if (!authorizationCache.getAuthorizedLogbookId().contains(lId)) {
                                // notify the error on logbook authorization
                                var logbook = logbookService.getLogbook(lId);
                                throw LogbookNotAuthorized.logbookAuthorizedBuilder()
                                        .errorCode(-1)
                                        .logbookName(logbook.name())
                                        .errorDomain("EntriesController::search")
                                        .build();
                            }

                        }
                );
            }
        } else {
            // if user is root we can use all logbook
            authorizationCache.setAuthorizedLogbookId(logBooks.orElse(Collections.emptyList()));
        }
        return true;
    }
}
