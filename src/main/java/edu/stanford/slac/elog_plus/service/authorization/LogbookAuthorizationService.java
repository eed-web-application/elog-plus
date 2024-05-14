package edu.stanford.slac.elog_plus.service.authorization;

import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.NewTagDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.ShiftDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.UpdateLogbookDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.*;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

@Service
@AllArgsConstructor
public class LogbookAuthorizationService {
    private final AuthService authService;

    /**
     * Check for read authorization
     * @param authentication
     * @param logbookId
     * @return
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
     *
     * @param authentication
     * @param logbookId
     * @param updateLogbookDTO
     * @return
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

    public boolean authorizedOnCreateNewTag(
            Authentication authentication,
            String logbookId,
            NewTagDTO newTagDTO
    ){
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

    public boolean authorizedOnGetAllTag(
            Authentication authentication,
            String logbookId
    ){
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
    ){
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
}
