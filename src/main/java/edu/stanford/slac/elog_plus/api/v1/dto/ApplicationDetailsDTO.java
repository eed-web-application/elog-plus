package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Application details")
public record ApplicationDetailsDTO(
        @Schema(description = "The id of the application")
        String id,
        @Schema(description = "The name of the application")
        String name,
        @Schema(description = "The description of the application")
        String email,
        @Schema(description = "The token of the application")
        String token,
        @Schema(description = "The expiration of the token")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDate expiration,
        @Schema(description = "True if the application is managed internally by the backend")
        Boolean applicationManaged,
        @Schema(description = "The list of authorizations of the application")
        List<DetailsAuthorizationDTO> authorizations
) {}
