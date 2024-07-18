package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Group details")
public record GroupDetailsDTO(
        @Schema(description = "The id of the group")
        String id,
        @Schema(description = "The name of the group")
        String name,
        @Schema(description = "The description of the group")
        String description,
        @Schema(description = "The list of members of the group")
        List<UserDetailsDTO> members,
        @Schema(description = "The list of authorizations of the group")
        List<DetailsAuthorizationDTO> authorizations) {
}
