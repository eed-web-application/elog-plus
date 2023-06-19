package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/v1/hello")
@AllArgsConstructor
@Schema(description = "Api set for media management")
public class HelloWorldController {
    @GetMapping(
            path = "/world",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Download a content from a file entry")
    public ApiResultResponse<String>  helloWorld() throws Exception {
        return ApiResultResponse.of("hello world");
    }
}