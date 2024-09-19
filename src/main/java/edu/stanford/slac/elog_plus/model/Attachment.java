package edu.stanford.slac.elog_plus.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document()
public class Attachment {
    public enum PreviewProcessingState{
        Waiting,
        Processing,
        Error,
        PreviewNotAvailable,
        Completed
    }
    @Id
    private String id;
    /**
     * Field to store the file name
     */
    private String fileName;
    /**
     * Field to store the file content
     */
    private String contentType;
    /**
     * Field to store the file size
     */
    private String hasPreview;
    /**
     * Field to store the preview ID
     */
    private String previewID;
    /**
     * Field to store the reference information
     */
    private String referenceInfo;
    /**
     * Field to store the preview image
     */
    private byte[] miniPreview;
    /**
     * Field to mark the in use state
     */
    @Builder.Default
    private Boolean inUse = false;
    /**
     * Field to mark can be deleted state
     */
    @Builder.Default
    private Boolean canBeDeleted = false;
    /**
     * Field to mark the preview processing state
     */
    @Builder.Default
    private PreviewProcessingState previewState = PreviewProcessingState.Waiting;
    /**
     * Field to mark the creation date
     */
    @CreatedDate
    private LocalDateTime createdDate;
    /**
     * Field to mark processing
     */
    private String processingId;
    /**
     * Field to store the processing timestamp
     */
    private Date processingTimestamp;
}
