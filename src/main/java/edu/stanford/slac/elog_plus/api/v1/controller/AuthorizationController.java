package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.UpdateAuthorizationDTO;
import edu.stanford.slac.elog_plus.service.AuthorizationServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Log4j2
@Validated
@RestController("ElogPlusAuthorizationController")
@RequestMapping("/v1/authorizations")
@AllArgsConstructor
@Schema(description = "Set of api for authorization management")
public class AuthorizationController {
    private final AuthorizationServices authorizationServices;

    /**
     * Create new authorization
     *
     * @param authentication the authentication object
     * @param newAuthorizationDTO the new authorization to create
     * @return the result of the creation
     */
    @PostMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(description = "Create a new authorization for logbook resourceType")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @logbookAuthorizationService.canCreateNewAuthorization(#authentication, #newAuthorizationDTO)")
    public ApiResultResponse<Boolean> createNewAuthorization(
            Authentication authentication,
            @RequestBody @Valid NewAuthorizationDTO newAuthorizationDTO
    ) {
        authorizationServices.createNew(
                newAuthorizationDTO
        );
        return ApiResultResponse.of(true);
    }

    /**
     * Update an authorization
     *
     * @param authentication the authentication object
     * @param authorizationId the id of the authorization to update
     * @param updateAuthorizationDTO the new authorization to update
     * @return the result of the update
     */
    @PutMapping(
            path = "/{authorizationId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "Manage authorization for logbook user authorization")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @logbookAuthorizationService.canUpdateAuthorization(#authentication, #authorizationId, #newAuthorizationDTO)")
    public ApiResultResponse<Boolean> updateAuthorizationById(
            Authentication authentication,
            @PathVariable @NotNull String authorizationId,
            @RequestBody @Valid UpdateAuthorizationDTO updateAuthorizationDTO
    ) {
        authorizationServices.updateAuthorization(
                authorizationId,
                updateAuthorizationDTO
        );
        return ApiResultResponse.of(true);
    }

    /**
     * Delete an authorization
     *
     * @param authentication the authentication object
     * @param authorizationId the id of the authorization to delete
     * @return the result of the deletion
     */
    @DeleteMapping(
            path = "/{authorizationId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "Manage authorization for logbook user authorization")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @logbookAuthorizationService.canDeleteAuthorization(#authentication, #authorizationId)")
    public ApiResultResponse<Boolean> deleteAuthorizationById(
            Authentication authentication,
            @PathVariable @NotNull String authorizationId
    ) {
        authorizationServices.deleteAuthorization(
                authorizationId
        );
        return ApiResultResponse.of(true);
    }
}
