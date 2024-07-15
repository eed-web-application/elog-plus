package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "User details")
public record UserDetailsDTO (
    @Schema(description = "The name of the user")
    String name,
    @Schema(description = "The surname of the user")
    String surname,
    @Schema(description = "The email of the user")
    String email,
    @Schema(description = "The authorization of the user")
    List<UserAuthorizationDTO> authorization) {}
