package edu.stanford.slac.elog_plus.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

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
    @Field("LOGBOOK")
    @Indexed
    private String logbook;
    private String priority;
    private String segment;
    @TextIndexed
    @Field("TITLE")
    private String title;
    @TextIndexed
    @Field("TEXT")
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
    private List<String> followUp = new ArrayList<>();
    @Field("FILE_PS")
    private String filePs;
    @Field("FILE_PREVIEW")
    private String filePreview;
    @Indexed
    @CreatedDate
    private LocalDateTime loggedAt;
    @CreatedDate
    private LocalDateTime eventAt;
    @Version
    private Integer version;
}
