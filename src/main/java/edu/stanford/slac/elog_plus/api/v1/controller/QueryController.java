package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.service.LogService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static java.util.Arrays.asList;

@RestController()
@RequestMapping("/v1/search")
@AllArgsConstructor
@Schema(description = "Main set of api for the query on the elog data")
public class QueryController {
    private LogService logService;
    private QueryParameterConfigurationDTO queryParameterConfigurationDTO;
    @Schema(description = "Perform the query on all elog data, keeping in consideration all the mcc and physics elog data.")
    @PostMapping()
    public ApiResultResponse<QueryPagedResultDTO<LogDTO>> globalSearch(@RequestBody QueryParameterDTO parameterDTO) {
        return ApiResultResponse.of(
                logService.searchAll(parameterDTO)
        );
    }

    @GetMapping("/parameter")
    public ApiResultResponse<QueryParameterConfigurationDTO> getQueryParameter() {
        return ApiResultResponse.of(
                queryParameterConfigurationDTO
        );
    }
}
