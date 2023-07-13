package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.springframework.data.annotation.Version;

import java.time.LocalDateTime;
import java.util.List;
@Builder(toBuilder=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Identify the single elog record")
public record LogDTO(
        @Schema(description = "record primary key")
        String id,
        String supersedeBy,
        @Schema(description = "the type of the entry")
        String entryType,
        String filePs,
        String filePreview,
        String logbook,
        String priority,
        String segment,
        List<String> tags,
        String title,
        String text,
        String author,
        List<AttachmentDTO> attachments,
        List<LogDTO> followUp,
        LogDTO followingUp,
        List<LogDTO> history,
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime logDate,
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime commitDate,
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        LocalDateTime progDate) {
}
