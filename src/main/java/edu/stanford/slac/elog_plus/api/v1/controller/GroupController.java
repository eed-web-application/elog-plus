package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.*;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import edu.stanford.slac.elog_plus.api.v1.dto.GroupDetailsDTO;
import edu.stanford.slac.elog_plus.service.AuthorizationServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.any;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

@Validated
@RestController()
@RequestMapping("/v1/group")
@AllArgsConstructor
@Schema(description = "Api for authentication information")
public class GroupController {
    private final AuthorizationServices authorizationServices;
    AuthService authService;
    PeopleGroupService peopleGroupService;

    /**
     * Create a new group
     *
     * @param authentication
     * @param newGroupDTO
     * @return the id of the new group
     */
    @PostMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Create new group")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @groupAuthorizationService.canCreateGroup(#authentication, #newGroupDTO)")
    public ApiResultResponse<String> createNewGroup(
            Authentication authentication,
            @RequestBody @Valid NewLocalGroupDTO newGroupDTO
    ) {
        return ApiResultResponse.of(
                authService.createLocalGroup(newGroupDTO)
        );
    }

    /**
     * Delete a group
     *
     * @param authentication
     * @param localGroupId
     * @return true if the group was deleted
     */
    @DeleteMapping(
            path = "/{localGroupId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Delete a local group")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @groupAuthorizationService.canDeleteGroup(#authentication, #localGroupId)")
    public ApiResultResponse<Boolean> deleteGroup(
            Authentication authentication,
            @Parameter(description = "The id of the local group to delete")
            @PathVariable @NotEmpty String localGroupId
    ) {
        // check authentication
        authService.deleteLocalGroup(localGroupId);
        return ApiResultResponse.of(true);
    }

    /**
     * Update a group
     *
     * @param authentication
     * @param localGroupId
     * @param updateGroupDTO
     * @return true if the group was updated
     */
    @PutMapping(
            path = "/{localGroupId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Update a local group")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @groupAuthorizationService.canUpdateGroup(#authentication, #localGroupId)")
    public ApiResultResponse<Boolean> updateGroup(
            Authentication authentication,
            @Parameter(description = "The id of the local group to update")
            @PathVariable @NotEmpty String localGroupId,
            @RequestBody @Valid UpdateLocalGroupDTO updateGroupDTO
    ) {
        // check authentication
        authService.updateLocalGroup(localGroupId, updateGroupDTO);
        return ApiResultResponse.of(true);
    }

    /**
     * Find a local group using an id
     *
     * @param authentication
     * @param localGroupId
     * @return the local group
     */
    @GetMapping(
            path = "/{localGroupId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(
            summary = "Find a local group using an id"
    )
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    @PostAuthorize("@groupAuthorizationService.applyFilterOnGroup(returnObject, authentication)")
    public ApiResultResponse<GroupDetailsDTO> findGroupById(
            Authentication authentication,
            @Parameter(description = "The id of the local group to find")
            @PathVariable @Valid String localGroupId,
            @Parameter(description = "Include members")
            @RequestParam("includeMembers") Optional<Boolean> includeMembers,
            @Parameter(description = "Include authorizations")
            @RequestParam("includeAuthorizations") Optional<Boolean> includeAuthorizations
    ) {
        return ApiResultResponse.of(
                authorizationServices.findGroup(localGroupId, includeMembers.orElse(false),includeAuthorizations.orElse(false))
        );
    }

    /**
     * Find the local group using a query parameter
     *
     * @param authentication
     * @param anchorId
     * @param contextSize
     * @param limit
     * @param search
     * @return the list of groups found
     */
    @GetMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Find the local group using a query parameter")
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    @PostAuthorize("@groupAuthorizationService.applyFilterOnGroupList(returnObject, authentication)")
    public ApiResultResponse<List<GroupDetailsDTO>> findLocalGroup(
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
                authorizationServices.findGroups(
                        LocalGroupQueryParameterDTO.builder()
                                .anchorID(anchorId.orElse(null))
                                .contextSize(contextSize.orElse(null))
                                .limit(limit.orElse(null))
                                .search(search.orElse(null))
                                .build(),
                        includeMembers.orElse(false),
                        includeAuthorizations.orElse(false)
                )
        );
    }

    /**
     * Get the list of users
     *
     * @param authentication                  the authentication
     * @param authorizationGroupManagementDTO the authorization group management
     * @return the list of users found
     */
    @PostMapping(
            path = "/authorize",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(
            summary = "Get current user information"
    )
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication) and @groupAuthorizationService.canManageGroupAuthorization(authentication, authorizationGroupManagementDTO)")
    public ApiResultResponse<Boolean> manageGroupManagementAuthorization(
            Authentication authentication,
            @RequestBody @Valid AuthorizationGroupManagementDTO authorizationGroupManagementDTO

    ) {
        authService.manageAuthorizationOnGroup(authorizationGroupManagementDTO);
        return ApiResultResponse.of(true);
    }
}
