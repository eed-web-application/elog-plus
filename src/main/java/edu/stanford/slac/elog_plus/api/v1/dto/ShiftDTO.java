package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * IS the shift during the day
 * @param name
 * @param from
 * @param to
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "DTO for the shift creation")
@Builder(toBuilder = true)
public record ShiftDTO(
        @Schema(description = "Unique identifier of the shift")
        String id,
        @Schema(description = "The logbook which the shift is associated")
        LogbookSummaryDTO logbook,
        @Schema(description = "Is the name of the shift")
        String name,
        @Schema(description = "Is the time where the shift start in the day with the form HH:MM")
        String from,
        @Schema(description = "Is the time where the shift ends in the day with the form HH:MM")
        String to
){}
