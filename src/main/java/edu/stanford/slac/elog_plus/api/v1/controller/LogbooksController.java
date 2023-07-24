package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.LogbookService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/v1/logbooks")
@AllArgsConstructor
@Schema(description = "Set of api that work on the logbooks")
public class LogbooksController {
    private LogbookService logbookService;

    @GetMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ApiResultResponse<List<LogbookDTO>> getAllLogbook() {
        return ApiResultResponse.of(
                logbookService.getAllLogbook()
        );
    }

    @PostMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<String> createLogbook(@RequestBody NewLogbookDTO newLogbookDTO) {
        return ApiResultResponse.of(
                logbookService.createNew(newLogbookDTO)
        );
    }

    @GetMapping(
            path = "/{logbookId}",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ApiResultResponse<LogbookDTO> getLogbook(@PathVariable String logbookId) {
        return ApiResultResponse.of(
                logbookService.getLogbook(logbookId)
        );
    }

    @PostMapping(
            path = "/{logbookId}/tags",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<String> createTag(
            @PathVariable String logbookId,
            @RequestBody NewTagDTO newTagDTO
    ) {
        return ApiResultResponse.of(
                logbookService.createNewTag(logbookId, newTagDTO)
        );
    }

    @GetMapping(
            path = "/{logbookId}/tags",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ApiResultResponse<List<TagDTO>> getAllTags(
            @PathVariable String logbookId
    ) {
        return ApiResultResponse.of(
                logbookService.getAllTags(logbookId)
        );
    }
}
