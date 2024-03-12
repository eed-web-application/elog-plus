package edu.stanford.slac.elog_plus.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document()
public class Entry {
    @Id
    private String id;
    private String originId;
    private String supersedeBy;
    private String entryType;
    @Indexed
    private List<String> logbooks;
    @Indexed
    private Summarizes summarizes;
    @TextIndexed
    private String title;
    @TextIndexed
    private String text;
    private String note;
    private String lastName;
    private String firstName;
    private String userName;
    @Indexed
    @Builder.Default
    // the list of the tags ids
    private List<String> tags = new ArrayList<>();
    @Indexed
    @Builder.Default
    private List<String> attachments = new ArrayList<>();
    @Indexed
    @Builder.Default
    private List<String> followUps = new ArrayList<>();
    @Indexed
    @Builder.Default
    private List<String> references = new ArrayList<>();
    @Builder.Default
    @Transient
    private List<String> referencedBy = new ArrayList<>();
    @Indexed
    @Builder.Default
    private LocalDateTime loggedAt = LocalDateTime.now();
    @Indexed
    private LocalDateTime eventAt;
    /**
     * The date and time when the activity was created.
     * This field is automatically populated with the creation date and time, using @CreatedDate annotation.
     */
    @CreatedDate
    private LocalDateTime createdDate;

    /**
     * The identifier of the user who created the activity.
     * This field stores the ID of the user who initially created the activity, using @CreatedBy annotation.
     */
    @CreatedBy
    private String createdBy;
    @Version
    private Integer version;
}
