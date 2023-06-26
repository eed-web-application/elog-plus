package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.QueryParameterConfigurationDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/v1/logbooks")
@AllArgsConstructor
@Schema(description = "Set of api that work on the logbooks")
public class Logbooks {
    private QueryParameterConfigurationDTO queryParameterConfigurationDTO;

    @GetMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ApiResultResponse<QueryParameterConfigurationDTO> getQueryParameter() {
        return ApiResultResponse.of(
                queryParameterConfigurationDTO
        );
    }
}
