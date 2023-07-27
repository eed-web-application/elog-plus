package edu.stanford.slac.elog_plus.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
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
    private String supersedeBy;
    private String entryType;
    @Indexed
    private String logbook;
    private String summarizeShift;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime summaryDate;
    @TextIndexed
    private String title;
    @TextIndexed
    private String text;
    private String lastName;
    private String firstName;
    private String userName;
    @Indexed
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    @Indexed
    @Builder.Default
    private List<String> attachments = new ArrayList<>();
    @Indexed
    @Builder.Default
    private List<String> followUps = new ArrayList<>();
    private String filePs;
    private String filePreview;
    @Indexed
    @CreatedDate
    private LocalDateTime loggedAt;
    private LocalDateTime eventAt;
    @Version
    private Integer version;
}
