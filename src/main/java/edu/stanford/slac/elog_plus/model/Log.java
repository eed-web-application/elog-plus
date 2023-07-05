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
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document()
public class Log {
    @Id
    private String id;
    private String supersedeBy;
    @Field("ENTRY_TYPE")
    private String entryType;
    @Field("LOGBOOK")
    @Indexed
    private String logbook;
    @Field("PRIORITY")
    private String priority;
    @Field("SEGMENT")
    private String segment;
    @TextIndexed
    @Field("TITLE")
    private String title;
    @TextIndexed
    @Field("TEXT")
    private String text;
    @Field("LASTNAME")
    private String lastName;
    @Field("FIRSTNAME")
    private String firstName;
    @Field("USERNAME")
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
    @Field("LOGDATE")
    private LocalDateTime logDate;
    @CreatedDate
    @Field("COMMITDATE")
    private LocalDateTime commitDate;
    @CreatedDate
    @Field("PROGDATE")
    private LocalDateTime progDate;
    @Version
    private Integer version;
}
