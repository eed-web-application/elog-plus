package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.TagDTO;
import edu.stanford.slac.elog_plus.service.LogbookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Read;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static java.util.Collections.emptyList;

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
    @PreAuthorize("@baseAuthorizationService.checkAuthenticated(#authentication)")
    public ApiResultResponse<List<TagDTO>> getAllTags(
            @Parameter(name = "logbooks", description = "The logbooks for filter the tags")
            @RequestParam("logbooks") Optional<List<String>> logbooks,
            Authentication authentication
    ) {
        // filter all readable
        Set<String> filteredLogbook = new HashSet<>(logbooks.orElse(new ArrayList<>()));

        if (!authService.checkForRoot(authentication)) {
            // get all authorized logbook authorizations
            List<AuthorizationDTO> authOnLogbook = authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                    authentication.getCredentials().toString(),
                    Read,
                    "/logbook/",
                    Optional.empty()
            );
            // filter the logbook
            var authorizedLogbook = authOnLogbook.stream()
                    .map(AuthorizationDTO::resource)
                    .map(s -> s.replace("/logbook/", ""))
                    .collect(Collectors.toCollection(HashSet::new));

            // add all readable-from-all logbook
            List<String> allPublicReadableLogbookIds = logbookService.getAllIdsReadAll();
            authorizedLogbook.addAll(allPublicReadableLogbookIds);

            // remove all logbook that are not in the list
            if(filteredLogbook.isEmpty()) {
                filteredLogbook.addAll(authorizedLogbook);
            } else {
                filteredLogbook.retainAll(authorizedLogbook);
            }

            // return all public readable logbook ids
            filteredLogbook.addAll(allPublicReadableLogbookIds);
            return ApiResultResponse.of(
                    // if the list is empty return an empty list because no one of the logbook wanted is authorized
                    filteredLogbook.isEmpty()?emptyList():logbookService.getAllTagsByLogbooksIds(filteredLogbook.stream().toList())
            );
        } else {
            return ApiResultResponse.of(
                    logbookService.getAllTagsByLogbooksIds(filteredLogbook.stream().toList())
            );
        }


    }
}
