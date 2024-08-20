package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Contains the minimal set of information of the entry")
@Builder(toBuilder = true)
public record EntrySummaryDTO(
        @Schema(description = "Unique identifier of the entry")
        String id,
        @Schema(description = "The logbooks which the entry is associated")
        List<LogbookSummaryDTO> logbooks,
        @Schema(description = "The title of the entry")
        String title,
        @Schema(description = "The user tha insert the entry")
        String loggedBy,
        @Schema(description = "The tags that describes the entry")
        List<TagDTO> tags,
        @Schema(description = "The attachment list of the entry")
        List<AttachmentDTO> attachments,
        @Schema(description = "The shift which the entry belong, if any")
        List<LogbookShiftDTO> shifts,
        @Schema(description = "Whether the entry is empty or not")
        boolean isEmpty,
        @Schema(description = "The entries referenced by this one")
        List<String> references,
        @Schema(description = "The entries that refer to this one")
        List<String> referencedBy,
        @Schema(description = "The id of the entry that is followUp for this the current entry is a follow ups")
        String followingUp,
        @Schema(description = "The list of entries that are follow ups of the current entry")
        List<String> followUps,
        @Schema(description = "The entry notes")
        String note,
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @Schema(description = "The timestamp when the entry has been created")
        LocalDateTime loggedAt,
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @Schema(description = "The timestamp when the event described by the entry happened")
        LocalDateTime eventAt
) {
}
