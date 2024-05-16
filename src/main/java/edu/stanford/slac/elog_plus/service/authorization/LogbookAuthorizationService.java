package edu.stanford.slac.elog_plus.service.authorization;

import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.*;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

/**
 * Service that handle the authorization for the logbook
 */
@Service
@AllArgsConstructor
public class LogbookAuthorizationService {
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

    public boolean applyUserAuthorization(Authentication authentication, @Valid List<LogbookUserAuthorizationDTO> authorizations) {
        // extract all logbook id fo check authorizations
        List<String> managedLogbookIds = authorizations.stream().map(LogbookUserAuthorizationDTO::logbookId).distinct().toList();
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbookAuthorizationService::applyUserAuthorization")
                        .build(),
                // can at least write the logbook which the entry belong
                () -> managedLogbookIds
                        .stream()
                        .allMatch
                                (
                                        logbookId -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                authentication,
                                                Admin,
                                                "/logbook/%s".formatted(logbookId)
                                        )
                                )
        );
        return true;
    }

    public boolean applyUserAuthorization(Authentication authentication, @NotNull String userId, @Valid List<LogbookAuthorizationDTO> authorizations) {
        // extract all logbook id fo check authorizations
        List<String> managedLogbookIds = authorizations.stream().map(LogbookAuthorizationDTO::logbookId).distinct().toList();
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbookAuthorizationService::applyUserAuthorization")
                        .build(),
                // can at least write the logbook which the entry belong
                () -> managedLogbookIds
                        .stream()
                        .allMatch
                                (
                                        logbookId -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                authentication,
                                                Admin,
                                                "/logbook/%s".formatted(logbookId)
                                        )
                                )
        );
        return true;
    }

    public boolean deleteUserAuthorization(Authentication authentication, @NotNull String logbookId) {
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbookAuthorizationService::deleteUserAuthorization")
                        .build(),
                // can at least write the logbook which the entry belong
                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Admin,
                        "/logbook/%s".formatted(logbookId)
                )

        );
        return true;
    }

    public boolean applyGroupAuthorization(Authentication authentication, @Valid List<LogbookGroupAuthorizationDTO> authorizations){
        // extract all logbook id fo check authorizations
        List<String> managedLogbookIds = authorizations.stream().map(LogbookGroupAuthorizationDTO::logbookId).distinct().toList();
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbookAuthorizationService::applyGroupAuthorization")
                        .build(),
                // can at least write the logbook which the entry belong
                () -> managedLogbookIds
                        .stream()
                        .allMatch
                                (
                                        logbookId -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                authentication,
                                                Admin,
                                                "/logbook/%s".formatted(logbookId)
                                        )
                                )
        );
        return true;
    }

    public boolean applyGroupAuthorization(Authentication authentication, @NotNull String groupId, @Valid List<LogbookAuthorizationDTO> authorizations){
        // extract all logbook id fo check authorizations
        List<String> managedLogbookIds = authorizations.stream().map(LogbookAuthorizationDTO::logbookId).distinct().toList();
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbookAuthorizationService::applyGroupAuthorization")
                        .build(),
                // can at least write the logbook which the entry belong
                () -> managedLogbookIds
                        .stream()
                        .allMatch
                                (
                                        logbookId -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                authentication,
                                                Admin,
                                                "/logbook/%s".formatted(logbookId)
                                        )
                                )
        );
        return true;
    }

    public boolean deleteGroupAuthorization(Authentication authentication, @NotNull String logbookId){
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbookAuthorizationService::deleteGroupAuthorization")
                        .build(),
                // can at least write the logbook which the entry belong
                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Admin,
                        "/logbook/%s".formatted(logbookId)
                )
        );
        return true;
    }
}
