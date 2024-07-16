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
@Schema(description = "New authorization for a user on a logbook")
public record NewAuthorizationDTO(
        @Schema(description = "The resource id that is authorized")
        String resourceId,
        @Schema(description = "The resource type that is authorized")
        ResourceTypeDTO resourceType,
        @Schema(description = "The owner id of the authorization")
        String ownerId,
        @Schema(description = "The owner type of the authorization")
        AuthorizationOwnerTypeDTO ownerTypeDTO,
        @Schema(description = "The authorization type")
        AuthorizationTypeDTO authorizationType
) {}
