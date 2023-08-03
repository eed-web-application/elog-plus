package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public record NewShiftDTO(
        @NotNull
        @NotEmpty
        @Schema(description = "Is the name of the shift")
        String name,
        @NotNull
        @NotEmpty
        @Schema(description = "Is the time where the shift start in the day with the form HH:MM in UTC timezone")
        String from,
        @NotNull
        @NotEmpty
        @Schema(description = "Is the time where the shift ends in the day with the form HH:MM in UTC timezone")
        String to
){}
