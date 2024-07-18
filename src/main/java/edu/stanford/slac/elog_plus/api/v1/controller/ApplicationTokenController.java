package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthenticationTokenDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthenticationTokenQueryParameterDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.NewAuthenticationTokenDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.LocalGroupQueryParameterDTO;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
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

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

@Validated
@RestController()
@RequestMapping("/v1/application")
@AllArgsConstructor
@Schema(description = "Api for authentication information")
public class ApplicationTokenController {
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
    public ApiResultResponse<String> createNewAuthentication(
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

    /**
     * return all the application token
     */
    @PostMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Search into all application"
    )
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    @PostAuthorize("@applicationAuthorizationService.applyFilterOnApplicationList(returnObject, #authentication)")
    public ApiResultResponse<List<ApplicationDetailsDTO>> finAllApplication(
            Authentication authentication,
            @Parameter(name = "anchorId", description = "Is the id of an entry from where start the search")
            @RequestParam("anchorId") Optional<String> anchorId,
            @Parameter(name = "contextSize", description = "Include this number of entries before the startDate (used for highlighting entries)")
            @RequestParam("contextSize") Optional<Integer> contextSize,
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
                                .anchor(anchorId.orElse(null))
                                .context(contextSize.orElse(null))
                                .limit(limit.orElse(null))
                                .searchFilter(search.orElse(null))
                                .build(),
                        includeMembers.orElse(false),
                        includeAuthorizations.orElse(false)
                )
        );
    }

}
