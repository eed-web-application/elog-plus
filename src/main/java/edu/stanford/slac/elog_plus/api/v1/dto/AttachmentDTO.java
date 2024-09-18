package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.stanford.slac.elog_plus.model.Attachment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Is the description fo an attachment")
public record AttachmentDTO (
        @Schema(description = "The id of the attachment")
    String id,
    @Schema(description = "The name of the file")
    String fileName,
    @Schema(description = "The content type of the file")
    String contentType,
    @Schema(description = "The state of the preview processing")
    String previewState,
    @Schema(description = "The mini preview of the file")
    byte[] miniPreview
    ){}
