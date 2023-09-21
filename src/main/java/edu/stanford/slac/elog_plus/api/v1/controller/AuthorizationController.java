package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.AuthorizationType;
import edu.stanford.slac.elog_plus.api.v1.dto.GroupDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.PersonDTO;
import edu.stanford.slac.elog_plus.exception.NotAuthorized;
import edu.stanford.slac.elog_plus.service.AuthService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;

@Log4j2
@RestController()
@RequestMapping("/v1/auth")
@AllArgsConstructor
@Schema(description = "Api for authentication information")
public class AuthorizationController {
    AuthService authService;

    @GetMapping(
            path = "/me",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    //@Cacheable(value = "current-user-info", key = "#authentication.credentials")
    public ApiResultResponse<PersonDTO> me(Authentication authentication) {
        // check authentication
        assertion(
                () -> authService.checkAuthentication(authentication),
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("AuthorizationController::me")
                        .build()
        );
        return ApiResultResponse.of(
                authService.findPerson(
                        authentication
                )
        );
    }

    @GetMapping(
            path = "/users",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ApiResultResponse<List<PersonDTO>> findPeople(
            @Parameter(description = "is the prefix used to filter the people")
            @RequestParam() Optional<String> search,
            Authentication authentication
    ) {
        // check authenticated
        assertion(
                () -> authService.checkAuthentication(authentication),
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateShift")
                        .build()
        );
        // assert that all the user that are root or admin of whatever resource
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("AuthorizationController::findPeople")
                        .build(),
                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        AuthorizationType.Admin,
                        "/"
                )
        );
        return ApiResultResponse.of(
                authService.findPersons(
                        search.orElse("")
                )
        );
    }

    @GetMapping(
            path = "/groups",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ApiResultResponse<List<GroupDTO>> findGroups(
            @Parameter(description = "is the prefix used to filter the groups")
            @RequestParam() Optional<String> search,
            Authentication authentication
    ) {
        // check authenticated
        assertion(
                () -> authService.checkAuthentication(authentication),
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbooksController::updateShift")
                        .build()
        );
        // assert that all the user that are root or admin of whatever resource
        assertion(
                NotAuthorized
                        .notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("AuthorizationController::findPeople")
                        .build(),
                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        AuthorizationType.Admin,
                        "/"
                )

        );
        return ApiResultResponse.of(
                authService.findGroup(
                        search.orElse("")
                )
        );
    }
}
