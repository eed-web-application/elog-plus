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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Admin;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

@Log4j2
@RestController()
@RequestMapping("/v1/logbook")
@AllArgsConstructor
@Schema(description = "Set of api for user/group management on logbook")
public class LogbookAuthorizationController {
    private final RequestBodyService requestBodyBuilder;
    AuthService authService;
    LogbookService logbookService;

    @GetMapping(
            path = "/auth",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "Manage authorization for logbook user authorization")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<LogbookAuthorizationDTO>> getUserAuthorizationOnLogbooks(Authentication authentication) {
        return ApiResultResponse.of(
                logbookService.getAllUserAuthorizations(authentication)
        );
    }


    @GetMapping(
            path = "/{logbookId}/auth",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "Manage authorization for logbook user authorization")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<LogbookAuthorizationDTO>> getUserAuthorizationOnLogbooks(
            Authentication authentication,
            @PathVariable @NotNull String logbookId
    ) {
        return ApiResultResponse.of(
                logbookService.getAllUserAuthorizations(authentication, logbookId)
        );
    }

    @PostMapping(
            path = "/auth/user",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "Manage authorization for logbook user authorization")
    @PreAuthorize(
            "@baseAuthorizationService.checkAuthenticated(#authentication) and @logbookAuthorizationService.applyUserAuthorization(#authentication, #authorizations)"
    )
    public ApiResultResponse<Boolean> applyUsersAuthorizationOnLogbook
            (
                    Authentication authentication,
                    @RequestBody @Valid List<LogbookUserAuthorizationDTO> authorizations
            ) {
        // user can update the authorizations
        logbookService.applyUserAuthorizations(authorizations);
        return ApiResultResponse.of(true);
    }

    @PostMapping(
            path = "/auth/user/{userId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "Manage authorization for logbook user authorization")
    @PreAuthorize(
            "@baseAuthorizationService.checkAuthenticated(#authentication) and  @logbookAuthorizationService.applyUserAuthorization(#authentication, #userId, #authorizations)"
    )
    public ApiResultResponse<Boolean> applyUsersAuthorizationOnLogbook
            (
                    Authentication authentication,
                    @PathVariable @NotEmpty String userId,
                    @RequestBody @Valid List<LogbookAuthorizationDTO> authorizations
            ) {
        // user can update the authorizations
        logbookService.applyUserAuthorizations(userId, authorizations);
        return ApiResultResponse.of(true);
    }

    @DeleteMapping(
            path = "/{logbookId}/auth/user",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "Manage authorization for logbook user authorization")
    @PreAuthorize(
            "@baseAuthorizationService.checkAuthenticated(#authentication) and @logbookAuthorizationService.deleteUserAuthorization(#authentication, #logbookId)"
    )
    public ApiResultResponse<Boolean> deleteUsersAuthorizationOnLogbook
            (
                    Authentication authentication,
                    @PathVariable @NotNull String logbookId
            ) {
        // user can update the authorizations
        logbookService.deleteUsersLogbookAuthorization(logbookId);
        return ApiResultResponse.of(true);
    }

    @PostMapping(
            path = "/auth/group",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "Manage authorization for logbook group authorization")
    @PreAuthorize(
            "@baseAuthorizationService.checkAuthenticated(#authentication) and @logbookAuthorizationService.applyGroupAuthorization(#authentication, #authorizations)"
    )
    public ApiResultResponse<Boolean> applyGroupAuthorizationOnLogbook
            (
                    Authentication authentication,
                    @RequestBody @Valid List<LogbookGroupAuthorizationDTO> authorizations
            ) {
        // user can update the authorizations
        logbookService.applyGroupAuthorizations(authorizations);
        return ApiResultResponse.of(true);
    }

    @PostMapping(
            path = "/auth/group/{groupId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "Manage authorization for logbook group authorization")
    @PreAuthorize(
            "@baseAuthorizationService.checkAuthenticated(#authentication) and @logbookAuthorizationService.applyGroupAuthorization(#authentication, #groupId, #authorizations)"
    )
    public ApiResultResponse<Boolean> applyGroupAuthorizationOnLogbook
            (
                    Authentication authentication,
                    @PathVariable @NotNull String groupId,
                    @RequestBody @Valid List<LogbookAuthorizationDTO> authorizations
            ) {
        // user can update the authorizations
        logbookService.applyGroupAuthorizations(groupId, authorizations);
        return ApiResultResponse.of(true);
    }

    @DeleteMapping(
            path = "/{logbookId}/auth/group",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "Manage authorization for logbook user authorization")
    @PreAuthorize(
            "@baseAuthorizationService.checkAuthenticated(#authentication) and @logbookAuthorizationService.deleteGroupAuthorization(#authentication, #logbookId)"
    )
    public ApiResultResponse<Boolean> deleteGroupAuthorizationOnLogbook
            (
                    Authentication authentication,
                    @PathVariable @NotNull String logbookId
            ) {
        // user can update the authorizations
        logbookService.deleteGroupsLogbookAuthorization(logbookId);
        return ApiResultResponse.of(true);
    }
}
