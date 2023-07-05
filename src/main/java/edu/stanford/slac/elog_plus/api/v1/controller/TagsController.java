package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.NewTagDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.TagDTO;
import edu.stanford.slac.elog_plus.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/v1/tags")
@AllArgsConstructor
@Schema(description = "Main set of api for the tags management")
public class TagsController {
    private final TagService tagService;

    @PostMapping(
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(description = "Create a new tag")
    public ApiResultResponse<String> newTag(@RequestBody NewTagDTO newTag) {
        return ApiResultResponse.of(
                tagService.createTag(newTag)
        );
    }

    @GetMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(description = "Return all the tags")
    public ApiResultResponse<List<TagDTO>> getAllTags() {
        return ApiResultResponse.of(
                tagService.getAllTags()
        );
    }
}
