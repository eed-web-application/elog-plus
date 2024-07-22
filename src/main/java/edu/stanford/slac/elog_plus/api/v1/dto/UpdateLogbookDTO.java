package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Update a logbooks information")
public record UpdateLogbookDTO(
        @Schema(description = "The name of the logbooks")
        String name,
        @Schema(description = "The tags associated to the logbooks")
        List<TagDTO> tags,
        @Schema(description = "The shift associated to the logbooks")
        List<ShiftDTO> shifts) {
}
