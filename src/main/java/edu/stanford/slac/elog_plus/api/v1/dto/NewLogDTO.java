package edu.stanford.slac.elog_plus.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Builder
@Schema(description = "Is the model for the new ELog creation")
public record NewLogDTO (
    @Schema(description = "Is the logbook where the new log belong")
    String logbook,
    @Schema(description = "Is the segment associated to the log")
    String segment,
    @Schema(description = "The title of the log")
    String title,
    @Schema(description = "The content of the log")
    String text,
    @Schema(description = "The tags describes represent the log")
    List<String> tags,
    @Schema(description = "The list of the attachment of the log")
    List<String> attachments
){}
