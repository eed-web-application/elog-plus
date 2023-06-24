package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.Arrays.asList;

@RestController()
@RequestMapping("/v1/search")
@AllArgsConstructor
@Schema(description = "Main set of api for the query on the elog data")
public class QueryController {
    private LogService logService;
    private QueryParameterConfigurationDTO queryParameterConfigurationDTO;
    @PostMapping( produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(description = "Perform the query on all ELOG data")
    public ApiResultResponse<QueryPagedResultDTO<LogDTO>> globalSearch(@RequestBody QueryParameterDTO parameterDTO) {
        return ApiResultResponse.of(
                logService.searchAll(parameterDTO)
        );
    }

    @GetMapping(
            params = {"page", "size", "logbook", "date_to"},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(description = "Perform the query on all ELOG data")
    public ApiResultResponse<QueryPagedResultDTO<LogDTO>> globalSearch(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam("logbook") List<String> logBook,
            @RequestParam("date_to") LocalDateTime to) {
        return ApiResultResponse.of(
                logService.searchAll(
                        QueryParameterDTO
                                .builder()
                                .page(page)
                                .rowPerPage(size)
                                .logBook(logBook)
                                .to(to)
                                .build()
                )
        );
    }


    @GetMapping("/parameter")
    public ApiResultResponse<QueryParameterConfigurationDTO> getQueryParameter() {
        return ApiResultResponse.of(
                queryParameterConfigurationDTO
        );
    }
}
