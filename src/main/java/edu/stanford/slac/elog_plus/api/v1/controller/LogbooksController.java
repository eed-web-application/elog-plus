package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.NotAuthorized;
import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.service.AuthService;
import edu.stanford.slac.elog_plus.service.LogbookService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.elog_plus.exception.Utility.*;
import static edu.stanford.slac.elog_plus.model.Authorization.Type.*;

@RestController()
@RequestMapping("/v1/logbooks")
@AllArgsConstructor
@Schema(description = "Set of api that work on the logbooks")
public class LogbooksController {
    private AuthService authService;
    private LogbookService logbookService;

    @GetMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ApiResultResponse<List<LogbookDTO>> getAllLogbook(
            @Parameter(name = "includeAuthorizations", description = "If true the authorizations will be loaded for every logbook found")
            @RequestParam("includeAuthorizations") Optional<Boolean> includeAuthorizations,
            @Parameter(name = "filterForAuthorizationTypes", description = "Filter the logbook for authorizations types")
            @RequestParam("filterForAuthorizationTypes") Optional<String> authorizationType,
            Authentication authentication
    ) {
        //todo return logbook also for a specific authorizations level
        // check the user is authenticated
        assertion(
                () -> authService.checkAuthentication(authentication),
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::getAllLogbook")
                        .build()
        );
        if (authService.checkForRoot(authentication)) {
            // for the admin return all logbook
            return ApiResultResponse.of(
                    logbookService.getAllLogbook(includeAuthorizations)
            );
        } else {
            // get all the logbook where the user is authorized (all type of authorizations)
            List<AuthorizationDTO> authOnLogbook = authService.getAllAuthorizationForOwnerAuthTypeAndResourcePrefix(
                    authentication.getCredentials().toString(),
                    authorizationType.map(
                            Authorization.Type::valueOf
                    ).orElse(
                            Read
                    ),
                    "/logbook/"
            );
            return ApiResultResponse.of(
                    logbookService.getLogbook(
                            authOnLogbook.stream()
                                    .map(
                                            auth -> auth.resource().substring(
                                                    auth.resource().lastIndexOf("/") + 1
                                            )
                                    )
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
    public ApiResultResponse<String> createLogbook(@RequestBody NewLogbookDTO newLogbookDTO, Authentication authentication) {
        // check authorizations
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateLogbook")
                        .build(),
                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and an administrator
                () -> authService.checkForRoot(authentication)

        );
        return ApiResultResponse.of(
                logbookService.createNew(newLogbookDTO)
        );
    }

    @GetMapping(
            path = "/{logbookId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ApiResultResponse<LogbookDTO> getLogbook(
            @PathVariable String logbookId,
            @Parameter(name = "includeAuthorizations", description = "If true the authorizations will be loaded for every logbook found")
            @RequestParam("includeAuthorizations") Optional<Boolean> includeAuthorizations,
            Authentication authentication
    ) {
        // check authorizations
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateLogbook")
                        .build(),
                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and need to be an admin or root
                () -> authService.checkAuthorizationOForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Admin,
                        "/logbook/%s".formatted(logbookId)
                )
        );
        return ApiResultResponse.of(
                logbookService.getLogbook(logbookId, includeAuthorizations)
        );
    }

    @PutMapping(
            path = "/{logbookId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<Boolean> updateLogbook(
            @PathVariable String logbookId,
            @RequestBody UpdateLogbookDTO updateLogbookDTO,
            Authentication authentication) {
        // check authorizations
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateLogbook")
                        .build(),
                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and or can write or administer the logbook
                () -> authService.checkAuthorizationOForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Write,
                        "/logbook/%s".formatted(logbookId)
                )
        );
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
    public ApiResultResponse<String> createTag(
            @PathVariable String logbookId,
            @RequestBody NewTagDTO newTagDTO,
            Authentication authentication
    ) {
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::createTag")
                        .build(),
                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and or can write or administer the logbook
                () -> authService.checkAuthorizationOForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Admin,
                        "/logbook/%s".formatted(logbookId)
                )
        );
        return ApiResultResponse.of(
                logbookService.createNewTag(logbookId, newTagDTO)
        );
    }

    @GetMapping(
            path = "/{logbookId}/tags",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ApiResultResponse<List<TagDTO>> getAllTags(
            @PathVariable String logbookId,
            Authentication authentication
    ) {
        // check authorizations
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateLogbook")
                        .build(),
                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and can read, write or administer the logbook
                () -> authService.checkAuthorizationOForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Read,
                        "/logbook/%s".formatted(logbookId)
                )
        );
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
    public ApiResultResponse<Boolean> replaceShift(
            @PathVariable String logbookId,
            @RequestBody List<ShiftDTO> shiftReplacement,
            Authentication authentication
    ) {
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateShift")
                        .build(),
                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and can write or administer the logbook
                () -> authService.checkAuthorizationOForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Admin,
                        "/logbook/%s".formatted(logbookId)
                )
        );
        logbookService.replaceShift(logbookId, shiftReplacement);
        return ApiResultResponse.of(
                true
        );
    }
}
