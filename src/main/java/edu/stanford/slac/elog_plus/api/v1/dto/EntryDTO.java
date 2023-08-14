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
        String logbook,
        String summarizeShift,
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime summaryDate,
        List<String> tags,
        String title,
        String text,
        String loggedBy,
        List<AttachmentDTO> attachments,
        List<EntryDTO> followUps,
        EntryDTO followingUp,
        List<EntryDTO> history,
        @Schema(description = "The shift which the entry belong, if any match the event date")
        ShiftDTO shift,
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime loggedAt,
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime eventAt) {
}
