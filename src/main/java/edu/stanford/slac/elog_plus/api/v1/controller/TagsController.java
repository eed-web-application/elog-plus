package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.TagDTO;
import edu.stanford.slac.elog_plus.exception.NotAuthorized;
import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.service.AuthService;
import edu.stanford.slac.elog_plus.service.LogbookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.model.Authorization.Type.Read;

@RestController()
@RequestMapping("/v1/tags")
@AllArgsConstructor
@Schema(description = "Set of api that work on the tags")
public class TagsController {
    private AuthService authService;
    private LogbookService logbookService;

    @GetMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Return all tags that belong to logbook identified by a list of ids")
    public ApiResultResponse<List<TagDTO>> getAllTags(
            @Parameter(name = "logbooks", description = "The logbooks for filter the tags")
            @RequestParam("logbooks") Optional<List<String>> logbooks,
            Authentication authentication
    ) {
        // check if the user is authenticated
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("TagsController::getAllTags")
                        .build(),
                ()->authService.checkAuthentication(authentication)
        );

        // filter all readable
        List<LogbookDTO> allReadableLogbook = logbookService.getAllLogbook().stream()
                .filter(
                        lb-> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                authentication,
                                Read,
                                "/logbook/%s".formatted(lb.id())
                        )
                )
                .toList();

        return ApiResultResponse.of(
                logbookService.getAllTagsByLogbooksIds(logbooks.orElse(Collections.emptyList()))
        );
    }
}
