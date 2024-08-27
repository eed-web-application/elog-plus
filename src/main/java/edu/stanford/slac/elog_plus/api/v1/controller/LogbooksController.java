package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.service.LogbookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Read;
import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Write;
import static java.util.Collections.emptyList;


@RestController()
@RequestMapping("/v1/logbooks")
@AllArgsConstructor
@Schema(description = "Set of api that work on the logbooks")
public class LogbooksController {
    private AuthService authService;
    private LogbookService logbookService;

    /**
     * Get all the logbooks
     *
     * @param authentication        the authentication object
     * @param includeAuthorizations if true the authorizations will be loaded for every logbook found
     * @param authorizationType     filter the logbook for authorizations types
     * @return a list of logbooks
     */
    @GetMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Return all authorized logbook")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<LogbookDTO>> getAllLogbook(
            Authentication authentication,
            @Parameter(name = "includeAuthorizations", description = "If true the authorizations will be loaded for every logbook found")
            @RequestParam("includeAuthorizations") Optional<Boolean> includeAuthorizations,
            @Parameter(name = "filterForAuthorizationTypes", description = "Filter the logbook for authorizations types")
            @RequestParam("filterForAuthorizationTypes") Optional<AuthorizationTypeDTO> authorizationType
    ) {
        if (authService.checkForRoot(authentication)) {
            // for the admin return all logbook
            return ApiResultResponse.of(
                    logbookService.getAllLogbook(includeAuthorizations)
            );
        } else {
            AuthorizationTypeDTO requestAuthorization = authorizationType.orElse(Read);
            // get all the logbook where the user is authorized (all type of authorizations)
            List<AuthorizationDTO> authOnLogbook = authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                    authentication.getCredentials().toString(),
                    requestAuthorization,
                    "/logbook/",
                    Optional.empty()
            );
            List<String> authorizedLogbook = authOnLogbook.stream()
                    .map(
                            auth -> auth.resource().substring(
                                    auth.resource().lastIndexOf("/") + 1
                            )
                    )
                    .toList();
            // add all readAll logbook to care about no duplication
            List<String> readAllLogbook = emptyList();
            if(requestAuthorization == Read) {
                // for read authorization we need to consider also public readable logbook
                readAllLogbook = logbookService.getAllIdsReadAll();
            } else if(requestAuthorization == Write) {
                // for write authorization we need to consider also public writable logbook
                readAllLogbook = logbookService.getAllIdsWriteAll();
            }
            return ApiResultResponse.of(
                    logbookService.getLogbook(
                            // concat all the logbook and remove duplication
                            Stream.concat(authorizedLogbook.stream(), readAllLogbook.stream())
                                    .distinct()
                                    .toList(),
                            includeAuthorizations
                    )
            );
        }
    }

    @PostMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crete a new logbook")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @baseAuthorizationService.checkForRoot(#authentication)")
    public ApiResultResponse<String> createLogbook(
            Authentication authentication,
            @RequestBody @Valid NewLogbookDTO newLogbookDTO
    ) {
        return ApiResultResponse.of(
                logbookService.createNew(newLogbookDTO)
        );
    }

    @GetMapping(
            path = "/{logbookId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(
            summary = "Return a full logbook by id",
            description = """
                    Not all field are returned, for example authorization filed are filled depending on #includeAuthorizations
                    parameter that is optiona
                    """)
    @PreAuthorize(
            "@baseAuthorizationService.checkAuthenticated(#authentication) and @logbookAuthorizationService.authorizedReadOnLogbook(#authentication, #logbookId)")
    public ApiResultResponse<LogbookDTO> getLogbook(
            @Parameter(name = "logbookId", required = true, description = "Is the logbook id")
            @PathVariable @NotNull String logbookId,
            @Parameter(name = "includeAuthorizations", description = "If true the authorizations will be loaded for every logbook found")
            @RequestParam("includeAuthorizations") Optional<Boolean> includeAuthorizations,
            Authentication authentication
    ) {
        return ApiResultResponse.of(
                logbookService.getLogbook(logbookId, includeAuthorizations)
        );
    }

    @PutMapping(
            path = "/{logbookId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize(
            "@baseAuthorizationService.checkAuthenticated(#authentication) and @logbookAuthorizationService.authorizedUpdateOnLogbook(#authentication, #logbookId, #updateLogbookDTO)")
    public ApiResultResponse<Boolean> updateLogbook(
            Authentication authentication,
            @PathVariable @NotNull String logbookId,
            @RequestBody @Valid UpdateLogbookDTO updateLogbookDTO) {
        // update the logbook
        logbookService.update(logbookId, updateLogbookDTO);
        return ApiResultResponse.of(
                true
        );
    }

    @PostMapping(
            path = "/{logbookId}/tags",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(
            "@baseAuthorizationService.checkAuthenticated(#authentication) and " +
                    "@logbookAuthorizationService.authorizedOnCreateNewTag(#authentication, #logbookId, #newTagDTO)")
    public ApiResultResponse<String> createTag(
            Authentication authentication,
            @PathVariable @NotNull String logbookId,
            @RequestBody @Valid NewTagDTO newTagDTO
    ) {
        return ApiResultResponse.of(
                logbookService.createNewTag(logbookId, newTagDTO)
        );
    }

    @GetMapping(
            path = "/{logbookId}/tags",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @PreAuthorize(
            "@baseAuthorizationService.checkAuthenticated(#authentication) and " +
                    "@logbookAuthorizationService.authorizedOnGetAllTag(#authentication, #logbookId)"
    )
    public ApiResultResponse<List<TagDTO>> getAllTags(
            Authentication authentication,
            @PathVariable @NotNull String logbookId
    ) {
        return ApiResultResponse.of(
                logbookService.getAllTags(logbookId)
        );
    }

    @PutMapping(
            path = "/{logbookId}/shifts",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(
            "@baseAuthorizationService.checkAuthenticated(#authentication) and " +
                    "@logbookAuthorizationService.authorizedOnReplaceShift(#authentication, #logbookId, #shiftReplacement)"
    )
    public ApiResultResponse<Boolean> replaceShift(
            Authentication authentication,
            @PathVariable @NotNull String logbookId,
            @RequestBody @Valid List<ShiftDTO> shiftReplacement
    ) {
        // replace shift
        logbookService.replaceShift(logbookId, shiftReplacement);
        return ApiResultResponse.of(
                true
        );
    }
}
