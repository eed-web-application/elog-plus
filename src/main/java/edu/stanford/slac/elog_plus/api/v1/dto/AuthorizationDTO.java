package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.ToString;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Is the description fo an attachment")
public record AuthorizationDTO(
        @Schema(description = "Is unique id of the authorization")
        String id,
        @Schema(description = "Is the type of the authorization")
        String authorizationType,
        @Schema(description = "Is the subject owner of the authorization")
        String owner
) {
}
