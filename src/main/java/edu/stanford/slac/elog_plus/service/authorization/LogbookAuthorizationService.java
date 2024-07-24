package edu.stanford.slac.elog_plus.service.authorization;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.service.AuthorizationServices;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.*;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.any;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

/**
 * Service that handle the authorization for the logbook
 */
@Service
@AllArgsConstructor
public class LogbookAuthorizationService {
    private final AuthorizationServices authorizationServices;
    private final AuthService authService;

    /**
     * Check for read authorization
     *
     * @param authentication the authentication object
     * @param logbookId      the logbook id
     * @return true if the user is authorized, false otherwise
     */
    public boolean authorizedReadOnLogbook(
            Authentication authentication,
            String logbookId
    ) {
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbookAuthorizationService::authorizedReadOnLogbook")
                        .build(),
                // needs be authenticated
                // and need to be an admin or root
                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Admin,
                        "/logbook/%s".formatted(logbookId)
                )
        );
        return true;
    }

    /**
     * Check for create authorization
     *
     * @param authentication      the authentication object
     * @param newAuthorizationDTO the new authorization dto
     * @return true if the user is authorized, false otherwise
     */
    public boolean canCreateNewAuthorization(Authentication authentication, NewAuthorizationDTO newAuthorizationDTO) {
        String resource = authorizationServices.getResource(newAuthorizationDTO);
        if (resource.equals("*") && newAuthorizationDTO.authorizationType() == Admin) {
            // check if user is a root user
            assertion(
                    ControllerLogicException
                            .builder()
                            .errorCode(-1)
                            .errorMessage("The resourceType '*' can only be granted by root users")
                            .errorDomain("AuthorizationServices::canCreateNewAuthorization")
                            .build(),
                    () -> authService.checkForRoot(authentication)
            );
        } else if (newAuthorizationDTO.resourceType() == ResourceTypeDTO.Logbook) {
            // check if current user is an admin for the logbook identified by the resourceType id
            assertion(
                    ControllerLogicException
                            .builder()
                            .errorCode(-1)
                            .errorMessage("User not authorize on logbook %s".formatted(newAuthorizationDTO.resourceId()))
                            .errorDomain("AuthorizationServices::canCreateNewAuthorization")
                            .build(),
                    () -> any(
                            // or is root
                            () -> authService.checkForRoot(authentication),
                            // or is an admin for the logbook
                            () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                    authentication,
                                    Admin,
                                    "/logbook/%s".formatted(newAuthorizationDTO.resourceId())
                            )

                    )
            );
        } else {
            throw ControllerLogicException.builder()
                    .errorCode(-1)
                    .errorMessage("bad authorized resources")
                    .errorDomain("AuthorizationServices::canCreateNewAuthorization")
                    .build();
        }
        return true;
    }

    /**
     * Check for update authorization
     *
     * @param authentication         the authentication object
     * @param updateAuthorizationDTO the update authorization dto
     * @return true if the user is authorized, false otherwise
     */
    public boolean canUpdateAuthorization(Authentication authentication, String authorizationId, UpdateAuthorizationDTO updateAuthorizationDTO) {
        var authorizationFound = authService.findAuthorizationById(authorizationId);
        if (authorizationFound.resource().equals("*") && authorizationFound.authorizationType() != Admin) {
            // root authorization cannot be updated
            throw ControllerLogicException
                    .builder()
                    .errorCode(-1)
                    .errorMessage("The resourceType '*' cannot be updated")
                    .errorDomain("AuthorizationServices::canUpdateAuthorization")
                    .build();
        } else if (authorizationFound.resource().startsWith("/logbook/")) {
            // check if current user is an admin for the logbook identified by the resourceType id
            assertion(
                    ControllerLogicException
                            .builder()
                            .errorCode(-1)
                            .errorMessage("User not authorize on logbook %s".formatted(authorizationFound.resource().substring(9)))
                            .errorDomain("AuthorizationServices::canUpdateAuthorization")
                            .build(),
                    () -> any(
                            // or is root
                            () -> authService.checkForRoot(authentication),
                            // or is an admin for the logbook
                            () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                    authentication,
                                    Admin,
                                    authorizationFound.resource())
                    )
            );
        } else {
            throw ControllerLogicException.builder()
                    .errorCode(-1)
                    .errorMessage("bad authorized resources")
                    .errorDomain("AuthorizationServices::canUpdateAuthorization")
                    .build();
        }
        return true;
    }

    /**
     * Check for delete authorization
     *
     * @param authentication  the authentication object
     * @param authorizationId the authorization id
     * @return true if the user is authorized, false otherwise
     */
    public boolean canDeleteAuthorization(Authentication authentication, String authorizationId) {
        var authorizationFound = authService.findAuthorizationById(authorizationId);
        if (authorizationFound.resource().equals("*") && authorizationFound.authorizationType() == Admin) {
            // check if user is a root user
            assertion(
                    ControllerLogicException
                            .builder()
                            .errorCode(-1)
                            .errorMessage("The resourceType '*' can only be deleted by root users")
                            .errorDomain("AuthorizationServices::canDeleteAuthorization")
                            .build(),
                    () -> authService.checkForRoot(authentication)
            );
        } else if (authorizationFound.resource().startsWith("/logbook/")) {
            // check if current user is an admin for the logbook identified by the resourceType id
            assertion(
                    ControllerLogicException
                            .builder()
                            .errorCode(-1)
                            .errorMessage("User not authorize on logbook %s".formatted(authorizationFound.resource().substring(9)))
                            .errorDomain("AuthorizationServices::canUpdateAuthorization")
                            .build(),
                    () -> any(
                            // or is root
                            () -> authService.checkForRoot(authentication),
                            // or is an admin for the logbook
                            () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                    authentication,
                                    Admin,
                                    authorizationFound.resource())
                    )
            );
        } else {
            throw ControllerLogicException.builder()
                    .errorCode(-1)
                    .errorMessage("bad authorized resources")
                    .errorDomain("AuthorizationServices::canDeleteAuthorization")
                    .build();
        }
        return true;
    }

    /**
     * Check for create authorization
     *
     * @param authentication   the authentication object
     * @param logbookId        the logbook id
     * @param updateLogbookDTO the update logbook dto
     * @return true if the user is authorized, false otherwise
     */
    public boolean authorizedUpdateOnLogbook(
            Authentication authentication,
            String logbookId,
            UpdateLogbookDTO updateLogbookDTO
    ) {
        // check authorizations
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbookAuthorizationService::authorizedUpdateOnLogbook")
                        .build(),
                // needs be authenticated
                // and or can write or administer the logbook
                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Write,
                        "/logbook/%s".formatted(logbookId)
                )
        );
        return true;
    }

    /**
     * Check for create new tag authorization
     *
     * @param authentication the authentication object
     * @param logbookId      the logbook id
     * @param newTagDTO      the new tag dto
     * @return true if the user is authorized, false otherwise
     */
    public boolean authorizedOnCreateNewTag(
            Authentication authentication,
            String logbookId,
            NewTagDTO newTagDTO
    ) {
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbookAuthorizationService::authorizedOnCreateNewTag")
                        .build(),
                // and or can write or administer the logbook
                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Admin,
                        "/logbook/%s".formatted(logbookId)
                )
        );
        return true;
    }

    /**
     * Check for read authorization
     *
     * @param authentication the authentication object
     * @param logbookId      the logbook id
     * @return true if the user is authorized, false otherwise
     */
    public boolean authorizedOnGetAllTag(
            Authentication authentication,
            String logbookId
    ) {
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbookAuthorizationService::authorizedOnGetAllTag")
                        .build(),
                // and can read, write or administer the logbook
                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Read,
                        "/logbook/%s".formatted(logbookId)
                )
        );
        return true;
    }

    public boolean authorizedOnReplaceShift(
            Authentication authentication,
            @NotNull String logbookId,
            @Valid List<ShiftDTO> shiftReplacement
    ) {
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbookAuthorizationService::authorizedOnReplaceShift")
                        .build(),
                // and can write or administer the logbook
                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Admin,
                        "/logbook/%s".formatted(logbookId)
                )
        );
        return true;
    }

    /**
     * Check for read authorization
     *
     * @param userList       the user list
     * @param authentication the authentication object
     * @return true if the user is authorized, false otherwise
     */
    public boolean applyFilterOnUserList(ApiResultResponse<List<UserDetailsDTO>> userList, Authentication authentication) {
        userList.setPayload
                (
                        userList
                                .getPayload()
                                .parallelStream()
                                .map
                                        (
                                                user -> completeUserAuthorization(user, authentication)
                                        )
                                .toList()
                );
        return true;
    }

    /**
     * Apply the authorization filter on the user
     * @param user
     * @param authentication
     * @return
     */
    public boolean applyFilterOnUser(ApiResultResponse<UserDetailsDTO> user, Authentication authentication) {
        user.setPayload
                (
                        completeUserAuthorization(user.getPayload(), authentication)

                );
        return true;
    }

    /**
     * Apply the authorization filter on the user
     *
     * @param user           the user to manage
     * @param authentication the authentication
     * @return the user with the authorization filter applied
     */
    private UserDetailsDTO completeUserAuthorization(UserDetailsDTO user, Authentication authentication) {
        List<DetailsAuthorizationDTO> filteredUserAuthorization = user
                .authorizations()
                .parallelStream()
                .filter
                        (
                                authorizationDTO -> {
                                    switch(authorizationDTO.resourceType()){
                                        case Logbook:
                                            return authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                    authentication,
                                                    Admin,
                                                    "/logbook/%s".formatted(authorizationDTO.resourceId())
                                            );
                                        case All:
                                            return true;
                                        default:
                                            return false;
                                    }
                                }
                        )
                .toList();
        return user
                .toBuilder()
                .authorizations(filteredUserAuthorization)
                .build();
    }
}
