package edu.stanford.slac.elog_plus.service.authorization;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.AuthorizationGroupManagementDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.NewLocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.UpdateLocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.ApplicationDetailsDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.GroupDetailsDTO;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.any;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

@Service
@AllArgsConstructor
public class ApplicationAuthorizationService {
    private final AuthService authService;

    public boolean canCreateApp(Authentication authentication, NewLocalGroupDTO newLocalGroupDTO) {
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("ApplicationAuthorizationService::canCreateApp")
                        .build(),
                // for now only root can manage application
                () -> authService.canManageGroup(authentication)

        );
        return true;
    }

    /**
     * Check if the user can delete the application
     *
     * @param authentication the authentication
     * @param applicationId  the application id
     * @return true if the user can delete the application
     */
    public boolean canDeleteApplication(Authentication authentication, String applicationId) {
        // assert that all the user that are root of whatever resource
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("ApplicationAuthorizationService::canDeleteApplication")
                        .build(),
                // is admin
                () -> authService.checkForRoot(authentication)
        );
        return true;
    }

    /**
     * Apply the filter on the application list
     *
     * @param foundEntryResult the result of the search
     * @param authentication   the authentication
     * @return true if the filter has been applied
     */
    public boolean applyFilterOnApplicationList(ApiResultResponse<List<ApplicationDetailsDTO>> foundEntryResult, Authentication authentication) {
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
     * @param application    the application to complete
     * @param authentication the authentication
     * @return the group with the authorization completed
     */
    private ApplicationDetailsDTO completeAuthorization(ApplicationDetailsDTO application, Authentication authentication) {
//        application.authorizations().parallelStream().map(
//                authorization -> authorization
//                        .toBuilder()
//                        .canEdit
//                                (
//                                        authService.checkForRoot(
//                                                authentication
//                                        )
//                                )
//                        .build()
//        );
        return application;
    }
}
