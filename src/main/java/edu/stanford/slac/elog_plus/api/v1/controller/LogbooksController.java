package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.NotAuthorized;
import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.model.Logbook;
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
import java.util.function.Supplier;

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
            @Parameter(name = "includeFollowUps", description = "If true the authorization will be loaded for every logbook found")
            @RequestParam("includeAuthorizations") Optional<Boolean> includeAuthorizations,
            Authentication authentication
    ) {
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
            // get all the logbook where the user is authorized (all type of authorization)
            List<AuthorizationDTO> authOnLogbook = authService.getAllAuthorization(
                    authentication.getCredentials().toString(),
                    List.of(Read, Write, Admin),
                    "/logbook/"
            );
            return ApiResultResponse.of(
                    logbookService.getLogbook(
                            authOnLogbook.stream()
                                    .map(
                                            auth -> auth.resource().substring(
                                                    auth.resource().lastIndexOf("/")
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
        // check authorization
        assertion(
                () -> all(
                        List.of(
                                // needs be authenticated
                                () -> authService.checkAuthentication(authentication),
                                // and an administrator
                                () -> authService.checkForRoot(authentication)
                        )
                ),
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateLogbook")
                        .build()
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
            @Parameter(name = "includeFollowUps", description = "If true the authorization will be loaded for every logbook found")
            @RequestParam("includeAuthorizations") Optional<Boolean> includeAuthorizations,
            Authentication authentication
    ) {
        // check authorization
        assertion(
                () -> all(
                        List.of(
                                // needs be authenticated
                                () -> authService.checkAuthentication(authentication),
                                // and
                                () -> any(
                                        List.of(
                                                // or is an admin
                                                () -> authService.checkForRoot(authentication),
                                                // or can write or administer the logbook
                                                () -> authService.checkAuthorizationOnResource(
                                                        authentication,
                                                        "/logbook/%s".formatted(logbookId),
                                                        List.of(
                                                                Admin
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateLogbook")
                        .build()
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
        // check authorization
        assertion(
                () -> all(
                        List.of(
                                // needs be authenticated
                                () -> authService.checkAuthentication(authentication),
                                // and
                                () -> any(
                                        List.of(
                                                // or is an admin
                                                () -> authService.checkForRoot(authentication),
                                                // or can write or administer the logbook
                                                () -> authService.checkAuthorizationOnResource(
                                                        authentication,
                                                        "/logbook/%s".formatted(logbookId),
                                                        List.of(
                                                                Admin
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateLogbook")
                        .build()
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
                () -> all(
                        List.of(
                                // needs be authenticated
                                () -> authService.checkAuthentication(authentication),
                                // and
                                () -> any(
                                        List.of(
                                                // or is an admin
                                                () -> authService.checkForRoot(authentication),
                                                // or can write or administer the logbook
                                                () -> authService.checkAuthorizationOnResource(
                                                        authentication,
                                                        "/logbook/%s".formatted(logbookId),
                                                        List.of(
                                                                Admin
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::createTag")
                        .build()
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
        // check authorization
        assertion(
                () -> all(
                        List.of(
                                // needs be authenticated
                                () -> authService.checkAuthentication(authentication),
                                // and
                                () -> any(
                                        List.of(
                                                // or is an admin
                                                () -> authService.checkForRoot(authentication),
                                                // or can read, write or administer the logbook
                                                () -> authService.checkAuthorizationOnResource(
                                                        authentication,
                                                        "/logbook/%s".formatted(logbookId),
                                                        List.of(
                                                                Read,
                                                                Write,
                                                                Admin
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateLogbook")
                        .build()
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
                () -> all(
                        List.of(
                                // needs be authenticated
                                () -> authService.checkAuthentication(authentication),
                                // and
                                () -> any(
                                        List.of(
                                                // or is an admin
                                                () -> authService.checkForRoot(authentication),
                                                // or can write or administer the logbook
                                                () -> authService.checkAuthorizationOnResource(
                                                        authentication,
                                                        "/logbook/%s".formatted(logbookId),
                                                        List.of(
                                                                Admin
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateShift")
                        .build()
        );
        logbookService.replaceShift(logbookId, shiftReplacement);
        return ApiResultResponse.of(
                true
        );
    }
}
