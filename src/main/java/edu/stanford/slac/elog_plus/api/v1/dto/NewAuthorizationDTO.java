package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.validation.ValidResourceTypeDependent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "New authorization for a user on a elog resourceType")
@ValidResourceTypeDependent
public record NewAuthorizationDTO(
        @Schema(description = "The resourceType id that need to be authorized")
        String resourceId,
        @NotNull
        @Schema(description = "The resourceType type that need to be authorized")
        ResourceTypeDTO resourceType,
        @NotNull
        @Schema(description = "The ownerId id of the authorization")
        String ownerId,
        @NotNull
        @Schema(description = "The ownerId type of the authorization [User, Group, Token]")
        AuthorizationOwnerTypeDTO ownerType,
        @NotNull
        @Schema(description = "The authorization type [Read, Write, Admin]")
        AuthorizationTypeDTO permission
) {}
