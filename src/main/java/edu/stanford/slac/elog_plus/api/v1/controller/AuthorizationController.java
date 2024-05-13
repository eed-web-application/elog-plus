package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookAuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookGroupAuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookUserAuthorizationDTO;
import edu.stanford.slac.elog_plus.service.LogbookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springdoc.core.service.RequestBodyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Admin;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

@Log4j2
@RestController()
@RequestMapping("/v1/auth")
@AllArgsConstructor
@Schema(description = "Set of api for user/group management on logbook")
public class AuthorizationController {
    private final RequestBodyService requestBodyBuilder;
    AuthService authService;
    LogbookService logbookService;

    @PostMapping(
            path = "/logbook/user",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @Operation(description = "Manage authorization for logbook user authorization")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<Boolean> applyUsersAuthorizationOnLogbook
            (
                    Authentication authentication,
                    @RequestBody @Valid List<LogbookUserAuthorizationDTO> authorizations
            ) {
        // extract all logbook id fo check authorizations
        List<String> managedLogbookIds = authorizations.stream().map(LogbookUserAuthorizationDTO::logbookId).distinct().toList();
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateUsersAuthorizationOnLogbook")
                        .build(),
                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and can at least write the logbook which the entry belong
                () -> managedLogbookIds
                        .stream()
                        .allMatch
                                (
                                        logbookId -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                authentication,
                                                Admin,
                                                "/logbook/%s".formatted(logbookId)
                                        )
                                )
        );

        // user can update the authorizations
        logbookService.applyUserAuthorizations(authorizations);
        return ApiResultResponse.of(true);
    }

    @PostMapping(
            path = "/logbook/user/{userId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @Operation(description = "Manage authorization for logbook user authorization")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<Boolean> applyUsersAuthorizationOnLogbook
            (
                    Authentication authentication,
                    @PathVariable @NotEmpty String userId,
                    @RequestBody @Valid List<LogbookAuthorizationDTO> authorizations
            ) {
        // extract all logbook id fo check authorizations
        List<String> managedLogbookIds = authorizations.stream().map(LogbookAuthorizationDTO::logbookId).distinct().toList();
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateUsersAuthorizationOnLogbook")
                        .build(),
                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and can at least write the logbook which the entry belong
                () -> managedLogbookIds
                        .stream()
                        .allMatch
                                (
                                        logbookId -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                authentication,
                                                Admin,
                                                "/logbook/%s".formatted(logbookId)
                                        )
                                )
        );

        // user can update the authorizations
        logbookService.applyUserAuthorizations(userId, authorizations);
        return ApiResultResponse.of(true);
    }

    @DeleteMapping(
            path = "/logbook/{logbookId}/user",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @Operation(description = "Manage authorization for logbook user authorization")
    @ResponseStatus(HttpStatus.OK)
    public ApiResultResponse<Boolean> deleteUsersAuthorizationOnLogbook
            (
                    Authentication authentication,
                    @PathVariable @NotNull String logbookId
            ) {
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateUsersAuthorizationOnLogbook")
                        .build(),
                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and can at least write the logbook which the entry belong
                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Admin,
                        "/logbook/%s".formatted(logbookId)
                )

        );

        // user can update the authorizations
        logbookService.deleteUsersLogbookAuthorization(logbookId);
        return ApiResultResponse.of(true);
    }

    @PostMapping(
            path = "/logbook/group",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @Operation(description = "Manage authorization for logbook user authorization")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<Boolean> applyGroupAuthorizationOnLogbook
            (
                    Authentication authentication,
                    @RequestBody @Valid List<LogbookGroupAuthorizationDTO> authorizations
            ) {
        // extract all logbook id fo check authorizations
        List<String> managedLogbookIds = authorizations.stream().map(LogbookGroupAuthorizationDTO::logbookId).distinct().toList();
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateUsersAuthorizationOnLogbook")
                        .build(),
                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and can at least write the logbook which the entry belong
                () -> managedLogbookIds
                        .stream()
                        .allMatch
                                (
                                        logbookId -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                authentication,
                                                Admin,
                                                "/logbook/%s".formatted(logbookId)
                                        )
                                )
        );

        // user can update the authorizations
        logbookService.applyGroupAuthorizations(authorizations);
        return ApiResultResponse.of(true);
    }

    @PostMapping(
            path = "/logbook/group/{groupId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @Operation(description = "Manage authorization for logbook user authorization")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<Boolean> applyGroupAuthorizationOnLogbook
            (
                    Authentication authentication,
                    @PathVariable @NotNull String groupId,
                    @RequestBody @Valid List<LogbookAuthorizationDTO> authorizations
            ) {
        // extract all logbook id fo check authorizations
        List<String> managedLogbookIds = authorizations.stream().map(LogbookAuthorizationDTO::logbookId).distinct().toList();
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateUsersAuthorizationOnLogbook")
                        .build(),
                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and can at least write the logbook which the entry belong
                () -> managedLogbookIds
                        .stream()
                        .allMatch
                                (
                                        logbookId -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                authentication,
                                                Admin,
                                                "/logbook/%s".formatted(logbookId)
                                        )
                                )
        );

        // user can update the authorizations
        logbookService.applyGroupAuthorizations(groupId, authorizations);
        return ApiResultResponse.of(true);
    }

    @DeleteMapping(
            path = "/logbook/{logbookId}/group",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @Operation(description = "Manage authorization for logbook user authorization")
    @ResponseStatus(HttpStatus.OK)
    public ApiResultResponse<Boolean> deleteGroupAuthorizationOnLogbook
            (
                    Authentication authentication,
                    @PathVariable @NotNull String logbookId
            ) {
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateUsersAuthorizationOnLogbook")
                        .build(),
                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and can at least write the logbook which the entry belong
                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Admin,
                        "/logbook/%s".formatted(logbookId)
                )

        );

        // user can update the authorizations
        logbookService.deleteGroupsLogbookAuthorization(logbookId);
        return ApiResultResponse.of(true);
    }
}
