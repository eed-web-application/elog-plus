package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Externalizable;
import java.io.Serializable;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Is the description fo an attachment")
public record AuthorizationDTO(
        @Schema(description = "Is unique id of the authorizations")
        String id,
        @Schema(description = "Is the type of the authorizations")
        String authorizationType,
        @Schema(description = "Is the subject owner of the authorizations")
        String owner,

        @Schema(description = "Is the type of the owner [User, Group, Application]")
        String ownerType,

        @Schema(description = "The resource eof the authorizations")
        String resource
)  implements Serializable {
}
