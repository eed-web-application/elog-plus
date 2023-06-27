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
    @Id
    String id;
    String fileName;
    String contentType;
    String hasPreview;
    @CreatedDate
    private LocalDateTime creationData;
}
