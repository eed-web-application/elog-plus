package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Identify a subject of an authorization with a specific level type")
public record LogbookOwnerAuthorizationDTO(
        String id,
        @NotEmpty
        @Schema(description = "The owner of the authorization")
        String owner,
        @NotEmpty
        @Schema(description = "The owner type of the authorization [User, Group, Token]")
        AuthorizationOwnerTypeDTO ownerType,
        @NotEmpty
        @Schema(description = "The type of authorization [Read, Write, Admin]")
        AuthorizationTypeDTO permission
){}
