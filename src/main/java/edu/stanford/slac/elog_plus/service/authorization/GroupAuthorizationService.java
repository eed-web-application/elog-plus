package edu.stanford.slac.elog_plus.service.authorization;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.AuthorizationGroupManagementDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.NewLocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.UpdateLocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.GroupDetailsDTO;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.any;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

@Service
@AllArgsConstructor
public class GroupAuthorizationService {
    private final AuthService authService;

    /**
     * Check if the user can create a group
     *
     * @param authentication     the authentication
     * @param newLocalGroupDTO the new group dto
     * @return true if the user can create the group
     */
    public boolean canCreateGroup(Authentication authentication, NewLocalGroupDTO newLocalGroupDTO) {
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("GroupAuthorizationService::canCreateGroup")
                        .build(),
                () -> any(
                        //is root
                        () -> authService.checkForRoot(authentication),
                        // or can manage group
                        () -> authService.canManageGroup(authentication)
                )
        );
        return true;
    }

    /**
     * Check if the user can delete the group
     *
     * @param authentication the authentication
     * @param groupId        the group id
     * @return true if the user can delete the group
     */
    public boolean canDeleteGroup(Authentication authentication, String groupId) {
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("GroupAuthorizationService::canDeleteGroup")
                        .build(),
                () -> any(
                        //is root
                        () -> authService.checkForRoot(authentication),
                        // or can manage group
                        () -> authService.canManageGroup(authentication)
                )
        );
        return true;
    }

    /**
     * Check if the user can update the group
     *
     * @param authentication     the authentication
     * @param updateLocalGroupDTO the group to update
     * @return true if the user can update the group
     */
    public boolean canUpdateGroup(Authentication authentication, UpdateLocalGroupDTO updateLocalGroupDTO) {
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("GroupAuthorizationService::canUpdateGroup")
                        .build(),
                () -> any(
                        //is root
                        () -> authService.checkForRoot(authentication),
                        // or can manage group
                        () -> authService.canManageGroup(authentication)
                )
        );
        return true;
    }

    /**
     * Check if the user can manage the group authorization
     *
     * @param authentication the authentication
     * @param authorizationGroupManagementDTO the group authorization management dto
     * @return true if the user can manage the group authorization
     */
    public boolean canManageGroupAuthorization(Authentication authentication, AuthorizationGroupManagementDTO authorizationGroupManagementDTO) {
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("GroupAuthorizationService::canManageGroupAuthorization")
                        .build(),
                () -> any(
                        //is root
                        () -> authService.checkForRoot(authentication),
                        // or can manage group
                        () -> authService.canManageGroup(authentication)
                )
        );
        return true;
    }

    /**
     * Apply the authorization filter on the group
     *
     * @param foundEntryResult the group found
     * @param authentication   the authentication
     * @return true if the filter was applied
     */
    public boolean applyFilterOnGroup(ApiResultResponse<GroupDetailsDTO> foundEntryResult, Authentication authentication) {
        foundEntryResult.setPayload
                (
                        completeAuthorization(foundEntryResult.getPayload(), authentication)
                );
        return true;
    }

    /**
     * Apply the authorization filter on the group list
     *
     * @param foundEntryResult the group list found
     * @param authentication   the authentication
     * @return true if the filter was applied
     */
    public boolean applyFilterOnGroupList(ApiResultResponse<List<GroupDetailsDTO>> foundEntryResult, Authentication authentication) {
        foundEntryResult.setPayload
                (
                        foundEntryResult
                                .getPayload()
                                .parallelStream()
                                .map
                                        (
                                                group -> completeAuthorization(group, authentication)
                                        )
                                .toList()
                );
        return true;
    }

    /**
     * Complete the authorization of the group
     *
     * @param group           the group to complete
     * @param authentication  the authentication
     * @return the group with the authorization completed
     */
    private GroupDetailsDTO completeAuthorization(GroupDetailsDTO group, Authentication authentication) {
//        group.authorizations().parallelStream().map(
//                authorization -> authorization
//                        .toBuilder()
//                        .canEdit
//                                (
//                                        authService.canManageGroup(authentication)
//                                )
//                        .build()
//        );
        return group;
    }


}
