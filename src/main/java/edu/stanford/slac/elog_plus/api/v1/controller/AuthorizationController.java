package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.UserDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Log4j2
@RestController()
@RequestMapping("/v1/auth")
@AllArgsConstructor
@Schema(description = "Api for authentication information")
public class AuthorizationController {
    @GetMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ApiResultResponse<UserDetails> me(Authentication authentication) {
        UserDetails details = null;
        if (authentication!=null && authentication.isAuthenticated()) {
            details = UserDetails
                    .builder()
                    .name(authentication.getPrincipal().toString())
                    .email(authentication.getCredentials().toString())
                    .build();

        } else {
            details = UserDetails
                    .builder()
                    .build();
        }
        return ApiResultResponse.of(details);
    }
}
