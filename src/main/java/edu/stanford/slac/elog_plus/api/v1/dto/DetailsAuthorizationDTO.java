package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Identify a subject of an authorization with a specific level type and resourceType type")
public record DetailsAuthorizationDTO(
        @Schema(description = "The id of the authorization")
        String id,
        @Schema(description = "The ownerId of the authorization")
        String ownerId,
        @Schema(description = "The owner name of the authorization")
        String ownerName,
        @Schema(description = "The owner type of the authorization [User, Group, Token]")
        AuthorizationOwnerTypeDTO ownerType,
        @Schema(description = "The id of the authorized resourceType")
        String resourceId,
        @Schema(description = "The resource type that is authorized")
        ResourceTypeDTO resourceType,
        @Schema(description = "The name of the authorized resourceType")
        String resourceName,
        @Schema(description = "The type of the authorization [Read, Write, Admin]")
        AuthorizationTypeDTO permission
){}
