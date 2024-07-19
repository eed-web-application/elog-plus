package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonQueryParameterDTO;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.UserDetailsDTO;
import edu.stanford.slac.elog_plus.service.AuthorizationServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/v1/users")
@AllArgsConstructor
@Schema(description = "Api for the users information")
public class UserController {
    private final AuthorizationServices authorizationServices;
    private final AuthService authService;

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
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "Search from all users")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    @PostAuthorize("@logbookAuthorizationService.applyFilterOnUserList(returnObject, authentication)")
    public ApiResultResponse<List<UserDetailsDTO>> findAllUsers(
            Authentication authentication,
            @Parameter(description = "The search string to find the user")
            @RequestParam(value = "search", required = false) Optional<String> search,
            @Parameter(description = "The size of the context to return")
            @RequestParam(value = "context", required = false) Optional<Integer> context,
            @Parameter(description = "The limit of the search")
            @RequestParam(value = "limit", required = false) Optional<Integer> limit,
            @Parameter(description = "The anchor of the search")
            @RequestParam(value = "anchor", required = false) Optional<String> anchor,
            @Parameter(description = "Include authorizations")
            @RequestParam("includeAuthorizations") Optional<Boolean> includeAuthorizations,
            @Parameter(description = "Include group inheritance")
            @RequestParam("includeInheritance") Optional<Boolean> includeInheritance
    ) {
        return ApiResultResponse.of(
                authorizationServices.findUsers(
                        PersonQueryParameterDTO.builder()
                                .searchFilter(search.orElse(null))
                                .context(context.orElse(null))
                                .limit(limit.orElse(null))
                                .anchor(anchor.orElse(null))
                                .build(),
                        includeAuthorizations.orElse(false),
                        includeInheritance.orElse(false)
                )
        );
    }

    @GetMapping(
            path = "/{userId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(description = "Get a single user by id")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    @PostAuthorize("@logbookAuthorizationService.applyFilterOnUser(returnObject, authentication)")
    public ApiResultResponse<UserDetailsDTO> findUserById(
            Authentication authentication,
            @Parameter(description = "The user id")
            @PathVariable String userId,
            @Parameter(description = "Include authorizations")
            @RequestParam("includeAuthorizations") Optional<Boolean> includeAuthorizations,
            @Parameter(description = "Include group inheritance")
            @RequestParam("includeInheritance") Optional<Boolean> includeInheritance
    ) {
        return ApiResultResponse.of(
                authorizationServices.findUser(userId, includeAuthorizations.orElse(false), includeInheritance.orElse(false))
        );
    }
}
