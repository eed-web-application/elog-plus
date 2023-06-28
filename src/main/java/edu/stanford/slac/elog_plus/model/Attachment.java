package edu.stanford.slac.elog_plus.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

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
    String id;
    String fileName;
    String contentType;
    String hasPreview;
    String previewID;
    @Builder.Default
    PreviewProcessingState previewState = PreviewProcessingState.Waiting;
    @CreatedDate
    private LocalDateTime creationData;
}
