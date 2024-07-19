package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthenticationTokenQueryParameterDTO;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.ApplicationDetailsDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewApplicationDTO;
import edu.stanford.slac.elog_plus.service.AuthorizationServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Validated
@RestController()
@RequestMapping("/v1/applications")
@AllArgsConstructor
@Schema(description = "Api for authentication information")
public class ApplicationController {
    private final AuthService authService;
    private final AuthorizationServices authorizationServices;


    /**
     * return all the application token
     */
    @PostMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create new authentication token",
            description = """
                    Create a new application, an application is created along with a jwt token that permit to access the REST without the needs of a user/password
                    it should be submitted in the http header along with the http request
                    """
    )
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @applicationAuthorizationService.canCreateApp(#authentication, #newAuthenticationTokenDTO)")
    public ApiResultResponse<String> createNewApplication(
            Authentication authentication,
            @Parameter(description = "Are the information to create the a new application")
            @RequestBody NewApplicationDTO newApplicationDTO
    ) {
        return ApiResultResponse.of(
                authorizationServices.createNewApplication(
                        newApplicationDTO
                )
        );
    }

    /**
     * return all the application token
     */
    @DeleteMapping(
            path = "/{applicationId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Delete an authentication token"
    )
    @PreAuthorize("@baseAuthorizationService.checkForRoot(#authentication) and @applicationAuthorizationService.canDeleteApplication(#authentication, #applicationId)")
    public ApiResultResponse<Boolean> deleteApplication(
            Authentication authentication,
            @Parameter(description = "Is the unique id of the authentication token")
            @PathVariable() String applicationId
    ) {
        authorizationServices.deleteApplication(applicationId);
        return ApiResultResponse.of(true);
    }

    @GetMapping(
            path = "/{applicationId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Delete an authentication token"
    )
    @PreAuthorize("@baseAuthorizationService.checkForRoot(#authentication) and @applicationAuthorizationService.canReadApplication(#authentication, #applicationId)")
    public ApiResultResponse<ApplicationDetailsDTO> findApplicationById(
            Authentication authentication,
            @Parameter(description = "Is the unique id of the authentication token")
            @PathVariable() String applicationId,
            @Parameter(description = "Include authorizations")
            @RequestParam("includeAuthorizations") Optional<Boolean> includeAuthorizations
    ){
        return ApiResultResponse.of(
                authorizationServices.getApplicationById(applicationId, includeAuthorizations.orElse(false))
        );
    }

    /**
     * return all the application token
     */
    @GetMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Search into all application"
    )
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    @PostAuthorize("@applicationAuthorizationService.applyFilterOnApplicationList(returnObject, #authentication)")
    public ApiResultResponse<List<ApplicationDetailsDTO>> findAllApplication(
            Authentication authentication,
            @Parameter(name = "anchorId", description = "Is the id of an entry from where start the search")
            @RequestParam("anchor") Optional<String> anchor,
            @Parameter(name = "context", description = "Include this number of entries before the startDate (used for highlighting entries)")
            @RequestParam("context") Optional<Integer> context,
            @Parameter(name = "limit", description = "Limit the number the number of entries after the start date.")
            @RequestParam(value = "limit") Optional<Integer> limit,
            @Parameter(name = "search", description = "Typical search functionality")
            @RequestParam(value = "search") Optional<String> search,
            @Parameter(description = "Include members")
            @RequestParam("includeMembers") Optional<Boolean> includeMembers,
            @Parameter(description = "Include authorizations")
            @RequestParam("includeAuthorizations") Optional<Boolean> includeAuthorizations
    ) {
        return ApiResultResponse.of(
                authorizationServices.findAllApplications(
                        AuthenticationTokenQueryParameterDTO.builder()
                                .anchor(anchor.orElse(null))
                                .context(context.orElse(null))
                                .limit(limit.orElse(null))
                                .searchFilter(search.orElse(null))
                                .build(),
                        includeMembers.orElse(false),
                        includeAuthorizations.orElse(false)
                )
        );
    }
}
