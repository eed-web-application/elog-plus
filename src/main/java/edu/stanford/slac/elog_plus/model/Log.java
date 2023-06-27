package edu.stanford.slac.elog_plus.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
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
    private String followedBy;
    @Field("ENTRY_TYPE")
    private String entryType;
    @Field("LOGBOOK")
    private String logbook;
    @Field("PRIORITY")
    private String priority;
    @Field("SEGMENT")
    private String segment;
    @Field("TITLE")
    private String title;
    @Field("TEXT")
    private String text;
    @Field("LASTNAME")
    private String lastName;
    @Field("FIRSTNAME")
    private String firstName;
    @Field("USERNAME")
    private String userName;
    private List<String> tags;
    private List<String> attachments;
    @Field("FILE_PS")
    private String filePs;
    @Field("FILE_PREVIEW")
    private String filePreview;
    @CreatedDate
    @Field("LOGDATE")
    private LocalDateTime logDate;
    @CreatedDate
    @Field("COMMITDATE")
    private LocalDateTime commitDate;
    @CreatedDate
    @Field("PROGDATE")
    private LocalDateTime progDate;
}
