package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonQueryParameterDTO;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.UpdateAuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.UserDetailsDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.LogbookMapperImpl;
import edu.stanford.slac.elog_plus.service.AuthorizationServices;
import edu.stanford.slac.elog_plus.service.LogbookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
import java.util.Optional;

@Log4j2
@RestController()
@RequestMapping("/v1")
@AllArgsConstructor
@Schema(description = "Set of api for authorization management")
public class LogbookAuthorizationController {
    private final RequestBodyService requestBodyBuilder;
    private final LogbookMapperImpl logbookMapperImpl;
    AuthService authService;
    AuthorizationServices authorizationServices;
    LogbookService logbookService;

    /**
     * Find users based on the query parameter
     *
     * @param authentication
     * @param searchFilter   the search string to find the user
     *                       (optional)
     * @param context        the size of the context to return
     *                       (optional)
     * @param limit          the limit of the search
     *                       (optional)
     * @param anchor         the anchor of the search
     *                       (optional)
     * @return the list of users found
     */
    @GetMapping(
            path = "/user",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "Create new authorization")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    //TODO: in the post filter remove the authorization on logbook that are not allowed to the current user
    public ApiResultResponse<List<UserDetailsDTO>> findAllUsers(
            Authentication authentication,
            @Parameter(description = "The search string to find the user")
            @RequestParam(value = "searchFilter", required = false) Optional<String> searchFilter,
            @Parameter(description = "The size of the context to return")
            @RequestParam(value = "context", required = false) Optional<Integer> context,
            @Parameter(description = "The limit of the search")
            @RequestParam(value = "limit", required = false) Optional<Integer> limit,
            @Parameter(description = "The anchor of the search")
            @RequestParam(value = "anchor", required = false) Optional<String> anchor
    ) {
        return ApiResultResponse.of(
                authorizationServices.findUsers(
                        PersonQueryParameterDTO.builder()
                                .searchFilter(searchFilter.orElse(null))
                                .context(context.orElse(null))
                                .limit(limit.orElse(null))
                                .anchor(anchor.orElse(null))
                                .build()
                )
        );
    }

    /**
     * Create new authorization
     *
     * @param authentication
     * @param newAuthorizationDTO the new authorization to create
     * @return the result of the creation
     */
    @PostMapping(
            path = "/authorization",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "Manage authorization for logbook user authorization")
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
     * @param authentication
     * @param authorizationId the id of the authorization to update
     * @param updateAuthorizationDTO the new authorization to update
     * @return the result of the update
     */
    @PutMapping(
            path = "/authorization/{authorizationId}",
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
     * @param authentication
     * @param authorizationId the id of the authorization to delete
     * @return the result of the deletion
     */
    @DeleteMapping(
            path = "/authorization/{authorizationId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
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

    //TODO: add local group and authentication token search
}
