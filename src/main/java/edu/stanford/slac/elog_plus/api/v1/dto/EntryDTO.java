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
@Builder(toBuilder=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Identify the single elog record")
public record EntryDTO(
        @Schema(description = "record primary key")
        String id,
        String supersedeBy,
        @Schema(description = "the type of the entry")
        String entryType,
        @Schema(description = "the logbooks where the entry belong")
        List<LogbookSummaryDTO> logbooks,
        @Schema(description = "the tags of the entry")
        List<TagDTO> tags,
        @Schema(description = "the title of the entry")
        String title,
        @Schema(description = "the text of the entry")
        String text,
        @Schema(description = "the author of the entry")
        String loggedBy,
        @Schema(description = "the attachments of the entry")
        List<AttachmentDTO> attachments,
        @Schema(description = "the follow up of the entry")
        List<EntryDTO> followUps,
        @Schema(description = "the entry that this one follow up")
        EntryDTO followingUp,
        @Schema(description = "the history of the entry")
        List<EntryDTO> history,
        @Schema(description = "The shift which the entry belong, if any match the event date")
        List<LogbookShiftDTO> shifts,
        Boolean referencesInBody,
        @Schema(description = "The entries referenced from this one")
        List<EntrySummaryDTO> references,
        @Schema(description = "The entries that reference this one")
        List<EntrySummaryDTO> referencedBy,
        @Schema(description = "the id of the shift where this entry is a summarization")
        String summarizeShift,
        @Schema(description = "the date of the summary")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime summaryDate,
        @Schema(description = "the date of the entry")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime loggedAt,
        @Schema(description = "the date of the event")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime eventAt) {
}
