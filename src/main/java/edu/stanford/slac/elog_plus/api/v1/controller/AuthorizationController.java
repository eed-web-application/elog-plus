package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookUserAuthorization;
import edu.stanford.slac.elog_plus.service.LogbookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Admin;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

@Log4j2
@RestController()
@RequestMapping("/v1/auth")
@AllArgsConstructor
@Schema(description = "Set of api for user/group management on logbook")
public class AuthorizationController {
    AuthService authService;
    LogbookService logbookService;

    @PostMapping(
            path = "/logbook/user",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}

    )
    @Operation(description = "Manage authorization for logbook user authorization")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<Boolean> updateUsersAuthorizationOnLogbook
            (
                    Authentication authentication,
                    @Valid List<LogbookUserAuthorization> authorizations
            ) {
        // extract all logbook id fo check authorizations
        List<String> managedLogbookIds = authorizations.stream().map(LogbookUserAuthorization::logbookId).distinct().toList();
        // check authenticated
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateUsersAuthorizationOnLogbook")
                        .build(),
                // needs be authenticated
                () -> authService.checkAuthentication(authentication),
                // and can at least write the logbook which the entry belong
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

        // user can update the authorizations
        logbookService.updateAuthorizations(authorizations);
        return ApiResultResponse.of(true);
    }
}
