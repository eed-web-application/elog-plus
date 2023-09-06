package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.service.AuthService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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
    public ApiResultResponse<PersonDTO> me(Authentication authentication) {
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
        return ApiResultResponse.of(
                authService.findPersons(
                        search.orElse(""),
                        authentication
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
        return ApiResultResponse.of(
                authService.findGroup(
                        search.orElse(""),
                        authentication
                )
        );
    }
}
