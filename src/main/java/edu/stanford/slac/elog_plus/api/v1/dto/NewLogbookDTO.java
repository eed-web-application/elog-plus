package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Identify the single logbooks")
public record NewLogbookDTO(
        @NotNull
        @NotEmpty
        @Schema(description = "The name of the logbook")
        String name,
        @Schema(description = "The read all open authorization of the logbook")
        Boolean readAll,
        @Schema(description = "The write all open authorization of the logbook")
        Boolean writeAll
) {
}
