package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/v1/logs")
@AllArgsConstructor
@Schema(description = "Main set of api for the query on the log data")
public class LogsController {
    private LogService logService;
    private QueryParameterConfigurationDTO queryParameterConfigurationDTO;
    @PostMapping( produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(description = "Perform the query on all log data")
    public ApiResultResponse<String> newLog(@RequestBody NewLogDTO newLog) {
        return ApiResultResponse.of(
                logService.createNew(newLog)
        );
    }

    @GetMapping(

            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Perform the query on all log data")
    public ApiResultResponse<QueryPagedResultDTO<LogDTO>> globalSearch(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("logbook") List<String> logBook) {
        return ApiResultResponse.of(
                logService.searchAll(
                        QueryParameterDTO
                                .builder()
                                .page(page)
                                .rowPerPage(size)
                                .logBook(logBook)
                                .build()
                )
        );
    }
}
